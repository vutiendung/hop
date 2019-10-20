/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.www.Carte;
import org.apache.hop.www.WebServer;

public class ClusterGenerator {

  public static final String TEST_CLUSTER_NAME = "test-cluster";

  /**
   * We use one master and 3 slaves
   */
  public static final SlaveServer[] LOCAL_TEST_SLAVES = new SlaveServer[] {
    new SlaveServer(
      "test-localhost-8585-master", "127.0.0.1", "8585", "cluster", "cluster", null, null, null, true ),
    new SlaveServer(
      "test-localhost-8586-slave", "127.0.0.1", "8586", "cluster", "cluster", null, null, null, false ),
    new SlaveServer(
      "test-localhost-8587-slave", "127.0.0.1", "8587", "cluster", "cluster", null, null, null, false ),
    new SlaveServer(
      "test-localhost-8588-slave", "127.0.0.1", "8588", "cluster", "cluster", null, null, null, false ), };

  private ClusterSchema clusterSchema;
  private List<Carte> carteList;

  public ClusterGenerator() throws HopException {
    this.clusterSchema = new ClusterSchema();
    this.clusterSchema.setName( TEST_CLUSTER_NAME );
    this.clusterSchema.getSlaveServers().addAll( Arrays.asList( LOCAL_TEST_SLAVES ) );
    this.clusterSchema.setSocketsCompressed( false );
    this.clusterSchema.setBasePort( "40000" );
    this.clusterSchema.setSocketsBufferSize( "2000" );
    this.clusterSchema.setSocketsFlushInterval( "5000" );

    this.carteList = new ArrayList<Carte>();
  }

  public void launchSlaveServers() throws Exception {

    // Launch the defined slave servers in a separate thread...
    //
    for ( SlaveServer slaveServer : LOCAL_TEST_SLAVES ) {
      final String hostname = slaveServer.getHostname();
      final int port = Const.toInt( slaveServer.getPort(), WebServer.PORT );
      CarteLauncher launcher = new CarteLauncher( hostname, port );
      Thread thread = new Thread( launcher );
      thread.setName( "Carte Launcher" + thread.getName() );
      thread.start();
      // Wait until the carte object is available...
      while ( launcher.getCarte() == null && !launcher.isFailure() ) {
        Thread.sleep( 100 );
      }
      // Keep a list of launched servers
      if ( launcher.getCarte() != null ) {
        carteList.add( launcher.getCarte() );
      }
      // If there is a failure, stop the servers already launched and throw the exception
      if ( launcher.isFailure() ) {
        stopSlaveServers();
        throw launcher.getException(); // throw the exception for good measure.
      }
    }
  }

  public void stopSlaveServers() throws Exception {
    for ( Carte carte : carteList ) {
      carte.getWebServer().stopServer();
    }
  }

  /**
   * @return the clusterSchema
   */
  public ClusterSchema getClusterSchema() {
    return clusterSchema;
  }

  /**
   * @param clusterSchema
   *          the clusterSchema to set
   */
  public void setClusterSchema( ClusterSchema clusterSchema ) {
    this.clusterSchema = clusterSchema;
  }
}
