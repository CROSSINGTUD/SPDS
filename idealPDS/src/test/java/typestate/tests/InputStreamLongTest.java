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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.InputStreamStateMachine;

public class InputStreamLongTest extends IDEALTestingFramework {

  @Test
  public void test1() throws IOException {
    InputStream inputStream = new FileInputStream("");
    inputStream.close();
    inputStream.read();
    mustBeInErrorState(inputStream);
  }

  @Test
  public void test2() throws IOException {
    InputStream inputStream = new FileInputStream("");
    inputStream.close();
    inputStream.close();
    inputStream.read();
    mustBeInErrorState(inputStream);
  }

  @Test
  public void test3() throws IOException {
    InputStream inputStream = new FileInputStream("");
    inputStream.read();
    inputStream.close();
    mustBeInAcceptingState(inputStream);
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new InputStreamStateMachine();
  }
}
