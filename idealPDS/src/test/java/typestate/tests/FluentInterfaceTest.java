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

import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.FileMustBeClosedStateMachine;
import typestate.test.helper.File;

public class FluentInterfaceTest extends IDEALTestingFramework {
  @Test
  public void fluentOpen() {
    File file = new File();
    file = file.open();
    mustBeInErrorState(file);
  }

  @Test
  public void fluentOpenAndClose() {
    File file = new File();
    file = file.open();
    mustBeInErrorState(file);
    file = file.close();
    mustBeInAcceptingState(file);
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new FileMustBeClosedStateMachine();
  }
}
