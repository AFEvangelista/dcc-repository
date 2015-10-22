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
package org.icgc.dcc.repository.collab;

import static org.icgc.dcc.repository.core.model.RepositorySource.COLLAB;

import org.icgc.dcc.repository.cloud.CloudImporter;
import org.icgc.dcc.repository.cloud.core.CloudFileProcessor;
import org.icgc.dcc.repository.cloud.s3.CloudS3BucketReader;
import org.icgc.dcc.repository.cloud.transfer.CloudTransferJobReader;
import org.icgc.dcc.repository.collab.s3.AWSClientFactory;
import org.icgc.dcc.repository.core.RepositoryFileContext;
import org.icgc.dcc.repository.core.model.RepositoryServers;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.val;

public class CollabImporter extends CloudImporter {

  /**
   * Constants.
   */
  private static final String DEFAULT_BUCKET_NAME = "oicr.icgc";
  private static final String DEFAULT_BUCKET_KEY_PREFIX = "data";
  private static final String DEFAULT_GIT_ORG_URL = "https://github.com/ICGC-TCGA-PanCancer";
  private static final String DEFAULT_GIT_REPO_URL = DEFAULT_GIT_ORG_URL + "/ceph_transfer_ops.git";

  public CollabImporter(@NonNull RepositoryFileContext context) {
    super(COLLAB, context);
  }

  @Override
  protected CloudTransferJobReader createJobReader() {
    val paths = ImmutableList.of("ceph-transfer-jobs-prod1/completed-jobs", "ceph-transfer-jobs-prod2/completed-jobs");
    return new CloudTransferJobReader(DEFAULT_GIT_REPO_URL, paths);
  }

  @Override
  protected CloudS3BucketReader createBucketReader() {
    val s3 = AWSClientFactory.createS3Client();
    return new CloudS3BucketReader(DEFAULT_BUCKET_NAME, DEFAULT_BUCKET_KEY_PREFIX, s3);
  }

  @Override
  protected CloudFileProcessor createFileProcessor() {
    val collabServer = RepositoryServers.getCollabServer();
    return new CloudFileProcessor(context, collabServer);
  }

}
