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

public class FileMustBeClosedInterfaceTest extends IDEALTestingFramework {
  @Test
  public void main() {
    File file = new File();
    Flow flow = (staticallyUnknown() ? new ImplFlow1() : new ImplFlow2());
    flow.flow(file);
    mayBeInErrorState(file);
    mayBeInAcceptingState(file);
  }

  @Test
  public void other() {
    File file = new File();
    if (staticallyUnknown()) {
      new ImplFlow1().flow(file);
      mustBeInErrorState(file);
    } else {
      new ImplFlow2().flow(file);
      mustBeInAcceptingState(file);
    }
    mayBeInAcceptingState(file);
    mayBeInErrorState(file);
  }

  public static class ImplFlow1 implements Flow {
    @Override
    public void flow(File file) {
      file.open();
    }
  }

  public static class ImplFlow2 implements Flow {

    @Override
    public void flow(File file) {
      file.close();
    }
  }

  private interface Flow {
    void flow(File file);
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new FileMustBeClosedStateMachine();
  }
}
