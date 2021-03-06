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
package org.icgc.dcc.repository.pcawg.core;

import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.common.core.util.stream.Streams.stream;
import static org.icgc.dcc.repository.pcawg.util.PCAWGArchives.getDccProjectCode;
import static org.icgc.dcc.repository.pcawg.util.PCAWGArchives.getSubmitterDonorId;

import java.util.Set;

import org.icgc.dcc.repository.core.RepositoryIdResolver;
import org.icgc.dcc.repository.pcawg.reader.PCAWGDonorArchiveReader;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PCAWGDonorIdResolver implements RepositoryIdResolver {

  @Override
  public Set<String> resolveIds() {
    val donors = readDonors();

    log.info("Collecting PCAWG study donor ids...");
    val submittedDonorIds = stream(donors)
        .map(donor -> qualifyDonorId(donor))
        .collect(toImmutableSet());
    log.info("Finish collecting PCAWG study donor ids");

    return submittedDonorIds;
  }

  @Override
  public Set<String> resolveIds(String esSearchUrl) {
    val donors = readDonors();

    log.info("Collecting PCAWG study donor ids...");
    val submittedDonorIds = stream(donors)
        .map(donor -> qualifyDonorId(donor))
        .collect(toImmutableSet());
    log.info("Finish collecting PCAWG study donor ids");

    return submittedDonorIds;
  }

  public static String qualifyDonorId(String projectCode, String submittedDonorId) {
    return projectCode + ":" + submittedDonorId;
  }

  private static String qualifyDonorId(ObjectNode donor) {
    return qualifyDonorId(getDccProjectCode(donor), getSubmitterDonorId(donor));
  }

  @SneakyThrows
  private Iterable<ObjectNode> readDonors() {
    val reader = new PCAWGDonorArchiveReader();
    return reader.readDonors();
  }

}
