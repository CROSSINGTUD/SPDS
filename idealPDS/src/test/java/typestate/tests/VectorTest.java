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

import java.util.Vector;
import org.junit.Ignore;
import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.VectorStateMachine;

public class VectorTest extends IDEALTestingFramework {

  @Test
  public void test1() {
    Vector s = new Vector();
    s.lastElement();
    mustBeInErrorState(s);
  }

  @Test
  public void test2() {
    Vector s = new Vector();
    s.add(new Object());
    s.firstElement();
    mustBeInAcceptingState(s);
  }

  @Test
  public void test3() {
    Vector v = new Vector();
    try {
      v.removeAllElements();
      v.firstElement();
    } catch (Exception e) {
      e.printStackTrace();
    }
    mayBeInErrorState(v);
  }

  @Test
  public void test4() {
    Vector v = new Vector();
    v.add(new Object());
    try {
      v.firstElement();
    } catch (Exception e) {
      e.printStackTrace();
    }
    mustBeInAcceptingState(v);
    if (staticallyUnknown()) {
      v.removeAllElements();
      v.firstElement();
      mustBeInErrorState(v);
    }
    mayBeInErrorState(v);
  }

  @Test
  public void test6() {
    Vector v = new Vector();
    v.add(new Object());
    mustBeInAcceptingState(v);
    if (staticallyUnknown()) {
      v.removeAllElements();
      v.firstElement();
      mustBeInErrorState(v);
    }
    mayBeInErrorState(v);
  }

  @Test
  public void test5() {
    Vector s = new Vector();
    s.add(new Object());
    if (staticallyUnknown()) s.firstElement();
    else s.elementAt(0);
    mustBeInAcceptingState(s);
  }

  static Vector v;

  public static void foo() {}

  @Ignore
  @Test
  public void staticAccessTest() {
    Vector x = new Vector();
    v = x;
    foo();
    v.firstElement();
    mustBeInErrorState(v);
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new VectorStateMachine();
  }
}
