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

import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.FileMustBeClosedStateMachineCallToReturn;
import typestate.test.helper.File;

public class SootSceneSetupTest extends IDEALTestingFramework {
  @Test
  public void simple() {
    File file = new File();
    file.open();
    mustBeInErrorState(file);
    file.close();
    mustBeInAcceptingState(file);
  }

  @Test
  @Ignore
  public void aliassimple() {
    File file = new File();
    File alias = file;
    alias.open();
    mustBeInErrorState(file);
    mustBeInErrorState(alias);
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new FileMustBeClosedStateMachineCallToReturn();
  }

  @Override
  public List<String> excludedPackages() {
    List<String> exlcuded = super.excludedPackages();
    exlcuded.add("typestate.test.helper.File");
    return exlcuded;
  }
}
