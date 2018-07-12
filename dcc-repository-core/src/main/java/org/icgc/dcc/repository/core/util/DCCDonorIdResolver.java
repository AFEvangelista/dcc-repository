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
package org.icgc.dcc.repository.core.util;

import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;

import java.util.Set;
import java.util.stream.Stream;

import org.icgc.dcc.repository.core.RepositoryIdResolver;
import org.icgc.dcc.repository.core.release.ReleaseClient;
import org.icgc.dcc.repository.core.release.ReleaseClient.Donor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DCCDonorIdResolver implements RepositoryIdResolver {

  @Override
  public Set<String> resolveIds() {
    return resolveDonors().map(this::formatId).collect(toImmutableSet());
  }

  @Override
  public Set<String> resolveIds(String esSearchUrl) {
    log.info("THIS IS MY ELASTICSEARCH URL " + esSearchUrl);
    return new ReleaseClient(esSearchUrl).getDonors().stream().map(this::formatId).collect(toImmutableSet());
  }

  private String formatId(Donor donor) {
    return donor.getProjectCode() + ":" + donor.getSubmittedDonorId();
  }

  private Stream<Donor> resolveDonors() {
    return new ReleaseClient().getDonors().stream();
  }

}
