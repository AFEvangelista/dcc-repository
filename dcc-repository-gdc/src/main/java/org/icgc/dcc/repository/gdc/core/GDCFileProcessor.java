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
package org.icgc.dcc.repository.gdc.core;

import static org.icgc.dcc.repository.gdc.util.GDCFiles.getAccess;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getAnalysisId;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getCaseId;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getCases;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getDataCategory;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getDataFormat;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getDataType;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getExperimentalStrategy;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getFileId;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getFileName;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getFileSize;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getIndexDataFormat;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getIndexFileId;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getIndexFileName;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getIndexFileSize;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getIndexFiles;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getIndexMd5sum;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getMd5sum;
import static org.icgc.dcc.repository.gdc.util.GDCFiles.getUpdatedDatetime;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import org.icgc.dcc.repository.core.RepositoryFileContext;
import org.icgc.dcc.repository.core.RepositoryFileProcessor;
import org.icgc.dcc.repository.core.model.RepositoryFile;
import org.icgc.dcc.repository.core.model.RepositoryFile.ReferenceGenome;
import org.icgc.dcc.repository.core.model.RepositoryServers.RepositoryServer;
import org.icgc.dcc.repository.gdc.util.GDCFiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.NonNull;
import lombok.val;

/**
 * Maps GDC files to ICGC repository file model.
 * 
 * @see https://wiki.oicr.on.ca/pages/viewpage.action?pageId=66946440
 */
public class GDCFileProcessor extends RepositoryFileProcessor {

  /**
   * Constants.
   */
  private static final ReferenceGenome GDC_REFERENCE_GENOME = new ReferenceGenome()
      .setGenomeBuild("GRCh38.p0")
      .setReferenceName("GRCh38.d1.vd1");

  /**
   * Metadata.
   */
  @NonNull
  private final RepositoryServer gdcServer;

  public GDCFileProcessor(RepositoryFileContext context, @NonNull RepositoryServer gdcServer) {
    super(context);
    this.gdcServer = gdcServer;
  }

  public Stream<RepositoryFile> process(Stream<ObjectNode> files) {
    return files.map(file -> createFile(file));
  }

  private RepositoryFile createFile(ObjectNode file) {
    val fileId = getFileId(file);
    val gdcFile = new RepositoryFile()
        .setId("FI" + fileId) // TODO: Enable this when business key is defined .setId(context.ensureFileId(fileId))
        .setObjectId(null);

    gdcFile.setAccess(getAccess(file));

    gdcFile.getAnalysisMethod()
        .setSoftware(null) // N/A
        .setAnalysisType(resolveAnalysisType(file));

    gdcFile.getDataCategorization()
        .setExperimentalStrategy(getExperimentalStrategy(file))
        .setDataType(resolveDataType(file));

    val dataBundleId = resolveDataBundleId(file);
    gdcFile.getDataBundle()
        .setDataBundleId(dataBundleId);

    gdcFile.setReferenceGenome(GDC_REFERENCE_GENOME);

    val fileCopy = gdcFile.addFileCopy()
        .setRepoDataBundleId(dataBundleId)
        .setRepoFileId(fileId)
        .setRepoDataSetId(null) // N/A
        .setFileFormat(getDataFormat(file))
        .setFileSize(getFileSize(file))
        .setFileName(getFileName(file))
        .setFileMd5sum(getMd5sum(file))
        .setLastModified(resolveLastModified(file))
        .setRepoType(gdcServer.getType().getId())
        .setRepoOrg(gdcServer.getSource().getId())
        .setRepoName(gdcServer.getName())
        .setRepoCode(gdcServer.getCode())
        .setRepoCountry(gdcServer.getCountry())
        .setRepoBaseUrl(gdcServer.getBaseUrl())
        .setRepoMetadataPath(gdcServer.getType().getMetadataPath())
        .setRepoDataPath(gdcServer.getType().getDataPath());

    for (val indexFile : getIndexFiles(file)) {
      val indexFileId = getIndexFileId(indexFile);
      fileCopy.getIndexFile()
          .setId(context.ensureFileId(indexFileId))
          .setObjectId(null) // N/A
          .setRepoFileId(indexFileId)
          .setFileName(getIndexFileName(indexFile))
          .setFileFormat(getIndexDataFormat(indexFile))
          .setFileSize(getIndexFileSize(indexFile))
          .setFileMd5sum(getIndexMd5sum(indexFile));
    }

    for (val caze : getCases(file)) {
      gdcFile.addDonor()
          .setSubmittedDonorId(getCaseId(caze))
          .setProjectCode(resolveProjectCode(caze));
    }

    return gdcFile;
  }

  private static String resolveProjectCode(@NonNull JsonNode caze) {
    return GDCFiles.getCaseProjectId(caze);
  }

  private static String resolveDataBundleId(@NonNull ObjectNode file) {
    return getAnalysisId(file);
  }

  private static String resolveDataType(@NonNull ObjectNode file) {
    return getDataCategory(file) + " " + getDataType(file);
  }

  private static String resolveAnalysisType(@NonNull ObjectNode file) {
    return GDCFiles.getAnalysisWorkflowType(file);
  }

  private static Long resolveLastModified(@NonNull ObjectNode file) {
    val text = getUpdatedDatetime(file);
    val accessor = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(text);

    return Instant.from(accessor).getEpochSecond();
  }

}
