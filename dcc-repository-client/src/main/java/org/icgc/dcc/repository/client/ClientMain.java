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
package org.icgc.dcc.repository.client;

import org.icgc.dcc.repository.client.core.RepositoryImporter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Entry point for the {@link RepositoryImporter}.
 */
@Slf4j
@SpringBootApplication
public class ClientMain {

  /**
   * Constants.
   */
  public static final String APPLICATION_NAME = "dcc-repository-client";
  public static final int SUCCESS_STATUS_CODE = 0;
  public static final int FAILURE_STATUS_CODE = 1;

  /**
   * Entry point into the application.
   * 
   * @param args command line arguments
   */
  public static void main(String... args) {
    try {
      run(args);
      System.exit(SUCCESS_STATUS_CODE);
    } catch (Exception e) {
      log.error("Unknown error: ", e);
      System.err.println("An an error occurred while processing. Please check the log for detailed error messages: "
          + e.getMessage());
      System.exit(FAILURE_STATUS_CODE);
    }
  }

  private static void run(String... args) {
    val importer = new SpringApplicationBuilder()
        .sources(ClientMain.class)
        .run(args)
        .getBean(RepositoryImporter.class);

    // Main point of execution
    importer.execute();
  }

}
