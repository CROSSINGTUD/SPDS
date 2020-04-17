/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package typestate.tests;

import java.io.IOException;
import java.net.HttpURLConnection;
import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.URLConnStateMachine;

public class URLConnTest extends IDEALTestingFramework {

  @Test
  public void test1() throws IOException {
    HttpURLConnection httpURLConnection =
        new HttpURLConnection(null) {

          @Override
          public void connect() throws IOException {
            // TODO Auto-generated method stub
            System.out.println("");
          }

          @Override
          public boolean usingProxy() {
            // TODO Auto-generated method stub
            return false;
          }

          @Override
          public void disconnect() {
            // TODO Auto-generated method stub

          }
        };
    httpURLConnection.connect();
    httpURLConnection.setDoOutput(true);
    mustBeInErrorState(httpURLConnection);
    httpURLConnection.setAllowUserInteraction(false);
    mustBeInErrorState(httpURLConnection);
  }

  @Test
  public void test2() throws IOException {
    HttpURLConnection httpURLConnection =
        new HttpURLConnection(null) {

          @Override
          public void connect() throws IOException {
            // TODO Auto-generated method stub
            System.out.println("");
          }

          @Override
          public boolean usingProxy() {
            // TODO Auto-generated method stub
            return false;
          }

          @Override
          public void disconnect() {
            // TODO Auto-generated method stub

          }
        };
    httpURLConnection.setDoOutput(true);
    httpURLConnection.setAllowUserInteraction(false);

    httpURLConnection.connect();
    mustBeInAcceptingState(httpURLConnection);
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new URLConnStateMachine();
  }
}
