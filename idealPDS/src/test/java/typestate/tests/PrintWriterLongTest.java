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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.PrintWriterStateMachine;

public class PrintWriterLongTest extends IDEALTestingFramework {

  @Test
  public void test1() throws FileNotFoundException {
    PrintWriter inputStream = new PrintWriter("");
    inputStream.close();
    inputStream.flush();
    mustBeInErrorState(inputStream);
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new PrintWriterStateMachine();
  }
}
