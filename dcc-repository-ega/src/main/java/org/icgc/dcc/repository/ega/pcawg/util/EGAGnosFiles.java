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
package org.icgc.dcc.repository.ega.pcawg.util;

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;

import org.icgc.dcc.repository.ega.pcawg.model.EGAGnosFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class EGAGnosFiles {

  public static ArrayNode getFiles(@NonNull EGAGnosFile gnosFile) {
    try {
      return (ArrayNode) at(gnosFile, "/ResultSet/Result/files/file");
    } catch (ClassCastException e) {
      log.warn("Should be array node!: {}", gnosFile.getAnalysisId());
      return DEFAULT.createArrayNode().add(at(gnosFile, "/ResultSet/Result/files/file"));
    }
  }

  public static long getFileSize(@NonNull JsonNode file) {
    return file.get("filesize").longValue();
  }

  public static String getFileName(@NonNull JsonNode file) {
    return file.get("filename").textValue();
  }

  private static JsonNode at(EGAGnosFile gnosFile, String path) {
    return gnosFile.getContents().at(path);
  }

}
