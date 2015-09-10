/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.repository.core;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import java.util.Map;
import java.util.Set;

import org.icgc.dcc.common.core.tcga.TCGAClient;
import org.icgc.dcc.id.client.core.IdClient;
import org.icgc.dcc.id.client.http.HttpIdClient;
import org.icgc.dcc.id.client.util.CachingIdClient;
import org.icgc.dcc.id.client.util.HashIdClient;
import org.icgc.dcc.repository.core.model.RepositorySource;
import org.icgc.dcc.repository.core.reader.RepositoryProjectReader;

import com.mongodb.MongoClientURI;

import lombok.Cleanup;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;
import lombok.experimental.Accessors;

@NoArgsConstructor(access = PRIVATE)
public final class RepositoryFileContextBuilder {

  /**
   * Constants.
   */
  private static final int DEFAULT_MONGO_PORT = 27017;
  private static final String MONGO_URI_TEMPLATE = "mongodb://localhost:%d/%s";
  private static final String DEFAULT_ID_SERVICE_URL = "http://hcache-dcc.oicr.on.ca:5391/";

  /**
   * Metadata.
   */
  @Setter
  @Accessors(chain = true, fluent = true)
  private MongoClientURI geneMongoUri = getLocalMongoClientUri("dcc-genome");
  @Setter
  @Accessors(chain = true, fluent = true)
  private MongoClientURI repoMongoUri = getLocalMongoClientUri("dcc-repository");
  @Setter
  @Accessors(chain = true, fluent = true)
  private String esUri = "es://localhost:9300";
  @Setter
  @Accessors(chain = true, fluent = true)
  private String idUrl = DEFAULT_ID_SERVICE_URL;
  @Setter
  @Accessors(chain = true, fluent = true)
  private Set<RepositorySource> sources = RepositorySource.all();
  @Setter
  @Accessors(chain = true, fluent = true)
  private boolean realIds = false;
  @Setter
  @Accessors(chain = true, fluent = true)
  private String authToken = null;
  @Setter
  @Accessors(chain = true, fluent = true)
  private RepositoryIdResolver pcawgIdResolver;

  public static RepositoryFileContextBuilder builder() {
    return new RepositoryFileContextBuilder();
  }

  @NonNull
  public RepositoryFileContext build() {
    val primarySites = getProjectPrimarySites(geneMongoUri);
    val idClient = createIdClient();
    val tcgaClient = createTCGAClient();

    return new RepositoryFileContext(repoMongoUri, esUri, sources, primarySites, idClient, tcgaClient, pcawgIdResolver);
  }

  private IdClient createIdClient() {
    return realIds ? new CachingIdClient(new HttpIdClient(idUrl, "", authToken)) : new HashIdClient();
  }

  private static TCGAClient createTCGAClient() {
    return new TCGAClient();
  }

  @SneakyThrows
  private static Map<String, String> getProjectPrimarySites(MongoClientURI geneMongoUri) {
    @Cleanup
    val projectReader = new RepositoryProjectReader(geneMongoUri);
    return projectReader.getPrimarySites();
  }

  public static final MongoClientURI getLocalMongoClientUri(String db) {
    return new MongoClientURI(format(MONGO_URI_TEMPLATE, DEFAULT_MONGO_PORT, db));
  }

}
