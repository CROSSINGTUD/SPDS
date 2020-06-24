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
package test.cases.typing;

import org.junit.Test;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class TypeConfusion extends AbstractBoomerangTest {
  @Test
  public void invokesInterface() {
    B b = new B();
    A a1 = new A();
    Object o = b;
    A a = null;
    if (staticallyUnknown()) {
      a = a1;
    } else {
      a = (A) o;
    }
    queryFor(a);
  }

  private static class A implements AllocatedObject {}

  private static class B {}
}
