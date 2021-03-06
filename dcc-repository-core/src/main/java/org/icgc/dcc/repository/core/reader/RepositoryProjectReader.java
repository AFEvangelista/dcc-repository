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
package org.icgc.dcc.repository.core.reader;

import static org.icgc.dcc.common.core.model.ReleaseCollection.PROJECT_COLLECTION;

import java.util.Map;

import org.icgc.dcc.repository.core.util.AbstractJongoComponent;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.mongodb.MongoClientURI;

import lombok.val;

public class RepositoryProjectReader extends AbstractJongoComponent {

  public RepositoryProjectReader(MongoClientURI mongoUri) {
    super(mongoUri);
  }

  public Map<String, String> getPrimarySites() {
    val map = ImmutableMap.<String, String> builder();
    eachDocument(PROJECT_COLLECTION.getId(), project -> {
      map.put(getProjectName(project), getPrimarySite(project));
    });

    return map.build();
  }

  private static String getProjectName(ObjectNode project) {
    return project.get("_project_id").textValue();
  }

  private static String getPrimarySite(ObjectNode project) {
    return project.get("primary_site").textValue();
  }

}
