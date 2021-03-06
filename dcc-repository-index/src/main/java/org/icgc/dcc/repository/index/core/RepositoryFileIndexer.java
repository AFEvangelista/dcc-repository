/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.repository.index.core;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Throwables.propagate;
import static org.icgc.dcc.common.core.util.Formats.formatCount;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.common.core.util.stream.Streams.stream;
import static org.icgc.dcc.dcc.common.es.TransportClientFactory.createClient;
import static org.icgc.dcc.repository.index.core.RepositoryFileIndexes.compareIndexDateDescending;
import static org.icgc.dcc.repository.index.core.RepositoryFileIndexes.getCurrentIndexName;
import static org.icgc.dcc.repository.index.core.RepositoryFileIndexes.getSettings;
import static org.icgc.dcc.repository.index.core.RepositoryFileIndexes.getTypeMapping;
import static org.icgc.dcc.repository.index.core.RepositoryFileIndexes.isRepoIndexName;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.elasticsearch.client.Client;
import org.icgc.dcc.dcc.common.es.DocumentWriterConfiguration;
import org.icgc.dcc.dcc.common.es.DocumentWriterFactory;
import org.icgc.dcc.dcc.common.es.core.DocumentWriter;
import org.icgc.dcc.repository.index.document.DonorTextDocumentProcessor;
import org.icgc.dcc.repository.index.document.FileCentricDocumentProcessor;
import org.icgc.dcc.repository.index.document.FileTextDocumentProcessor;
import org.icgc.dcc.repository.index.document.RepositoryDocumentProcessor;
import org.icgc.dcc.repository.index.model.DocumentType;
import org.icgc.dcc.repository.index.util.TarArchiveDocumentWriter;

import com.mongodb.MongoClientURI;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RepositoryFileIndexer implements Closeable {

  /**
   * Configuration.
   */
  @NonNull
  private final MongoClientURI mongoUri;
  @NonNull
  private final URI archiveUri;
  @NonNull
  private final String indexAlias;
  @NonNull
  private final String indexName;

  /**
   * Dependencies.
   */
  @NonNull
  private final Client client;
  private final DocumentWriter documentWriter;

  public RepositoryFileIndexer(@NonNull MongoClientURI mongoUri, @NonNull URI esUri, URI archiveUri,
      String indexAlias) {
    this.mongoUri = mongoUri;
    this.archiveUri = archiveUri;
    this.indexAlias = indexAlias;
    this.indexName = getCurrentIndexName(indexAlias);
    this.client = createClient(esUri.toString());
    this.documentWriter = createDocumentWriter(this.client, this.indexName);
  }

  public void indexFiles() {
    initializeIndex();
    indexDocuments();
    aliasIndex();
    pruneIndexes();
  }

  @Override
  public void close() throws IOException {
    documentWriter.close();
    client.close();
  }

  private void initializeIndex() {
    val indexClient = client.admin().indices();

    log.info("Checking index '{}' for existence...", indexName);
    val exists = indexClient.prepareExists(indexName)
        .execute()
        .actionGet()
        .isExists();

    if (exists) {
      log.info("Deleting index '{}'...", indexName);
      checkState(indexClient.prepareDelete(indexName)
          .execute()
          .actionGet()
          .isAcknowledged(),
          "Index '%s' deletion was not acknowledged", indexName);
    }

    try {
      log.info("Creating index '{}'...", indexName);
      checkState(indexClient
          .prepareCreate(indexName)
          .setSettings(getSettings().toString())
          .execute()
          .actionGet()
          .isAcknowledged(),
          "Index '%s' creation was not acknowledged!", indexName);

      for (val type : DocumentType.values()) {
        val typeName = type.getId();
        val source = getTypeMapping(typeName).toString();

        log.info("Creating index '{}' mapping for type '{}'...", indexName, typeName);
        checkState(indexClient.preparePutMapping(indexName)
            .setType(typeName)
            .setSource(source)
            .execute()
            .actionGet()
            .isAcknowledged(),
            "Index '%s' type mapping in index '%s' was not acknowledged for '%s'!",
            typeName, indexName);
      }
    } catch (Throwable t) {
      propagate(t);
    }
  }

  private void indexDocuments() {
    val watch = createStarted();

    @Cleanup
    val archiveWriter = createArchiveWriter();

    log.info("Indexing repository documents...");
    val repositoryCount = indexRepositoryDocuments(archiveWriter);
    log.info("Indexing file documents...");
    val fileCount = indexFileDocuments(archiveWriter);
    log.info("Indexing file text documents...");
    val fileTextCount = indexFileTextDocuments(archiveWriter);
    log.info("Indexing file donor documents...");
    val fileDonorCount = indexFileDonorDocuments(archiveWriter);

    log.info("Finished indexing {}, repository, {} file, {} file text and {} file donor documents in {}",
        formatCount(repositoryCount), formatCount(fileCount), formatCount(fileTextCount), formatCount(fileDonorCount),
        watch);
  }

  @SneakyThrows
  private int indexRepositoryDocuments(TarArchiveDocumentWriter archiveWriter) {
    @Cleanup
    val processor = new RepositoryDocumentProcessor(mongoUri, documentWriter, archiveWriter);
    return processor.process();
  }

  @SneakyThrows
  private int indexFileDocuments(TarArchiveDocumentWriter archiveWriter) {
    @Cleanup
    val processor = new FileCentricDocumentProcessor(mongoUri, documentWriter, archiveWriter);
    return processor.process();
  }

  @SneakyThrows
  private int indexFileTextDocuments(TarArchiveDocumentWriter archiveWriter) {
    @Cleanup
    val processor = new FileTextDocumentProcessor(mongoUri, documentWriter, archiveWriter);
    return processor.process();
  }

  @SneakyThrows
  private int indexFileDonorDocuments(TarArchiveDocumentWriter archiveWriter) {
    @Cleanup
    val processor = new DonorTextDocumentProcessor(mongoUri, documentWriter, archiveWriter);
    return processor.process();
  }

  @SneakyThrows
  private TarArchiveDocumentWriter createArchiveWriter() {
    val userName = archiveUri.getUserInfo();
    if (userName != null) {
      System.setProperty("HADOOP_USER_NAME", userName);
    }

    val fileSystem = FileSystem.get(archiveUri, new Configuration());
    val archive = new GZIPOutputStream(fileSystem.create(new Path(archiveUri)));

    return new TarArchiveDocumentWriter(indexName, archive);
  }

  @SneakyThrows
  private void aliasIndex() {
    // Remove existing alias
    val request = client.admin().indices().prepareAliases();
    for (val index : getIndexNames()) {
      request.removeAlias(index, indexAlias);
    }

    // Add new alias
    log.info("Assigning index alias {} to index {}...", indexAlias, indexName);
    request.addAlias(indexName, indexAlias);

    // Re-assign
    checkState(request
        .execute()
        .actionGet()
        .isAcknowledged(),
        "Assigning index alias '%s' to index '%s' was not acknowledged!",
        indexAlias, indexName);
  }

  private void pruneIndexes() {
    String[] staleRepoIndexNames =
        getIndexNames()
            .stream()
            .filter(isRepoIndexName(indexAlias))
            .sorted(compareIndexDateDescending(indexAlias))
            .skip(3) // Keep 3
            .toArray(size -> new String[size]);

    if (staleRepoIndexNames.length == 0) {
      return;
    }

    log.info("Pruning stale indexes '{}'...", Arrays.toString(staleRepoIndexNames));
    val indexClient = client.admin().indices();
    checkState(indexClient.prepareDelete(staleRepoIndexNames)
        .execute()
        .actionGet()
        .isAcknowledged(),
        "Index '%s' deletion was not acknowledged", Arrays.toString(staleRepoIndexNames));
  }

  private Set<String> getIndexNames() {
    val state = client.admin()
        .cluster()
        .prepareState()
        .execute()
        .actionGet()
        .getState();

    return stream(state.getMetaData().getIndices().keys())
        .map(key -> key.value)
        .collect(toImmutableSet());
  }

  private static DocumentWriter createDocumentWriter(Client client, String indexName) {
    val configuration = new DocumentWriterConfiguration().client(client).indexName(indexName);

    return DocumentWriterFactory.createDocumentWriter(configuration);
  }

}
