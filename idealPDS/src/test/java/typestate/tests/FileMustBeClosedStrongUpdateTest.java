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
import typestate.test.helper.ObjectWithField;

public class FileMustBeClosedStrongUpdateTest extends IDEALTestingFramework {
  @Test
  public void noStrongUpdatePossible() {
    File b = null;
    File a = new File();
    a.open();
    File e = new File();
    e.open();
    if (staticallyUnknown()) {
      b = a;
    } else {
      b = e;
    }
    b.close();
    mayBeInErrorState(a);
    mustBeInAcceptingState(b);
  }

  @Test
  public void aliasSensitive() {
    ObjectWithField a = new ObjectWithField();
    ObjectWithField b = a;
    File file = new File();
    file.open();
    a.field = file;
    File loadedFromAlias = b.field;
    loadedFromAlias.close();
    mustBeInAcceptingState(file);
    mustBeInAcceptingState(a.field);
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new FileMustBeClosedStateMachine();
  }
}
