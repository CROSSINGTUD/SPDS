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
package test.cases.fields;

import org.junit.Test;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class MeetPOITest extends AbstractBoomerangTest {

  @Test
  public void wrappedAlloc() {
    A e = new A();
    A g = e;
    wrapper(g);
    C h = e.b.c;
    queryFor(h);
  }

  private void wrapper(A g) {
    alloc(g);
  }

  private void alloc(A g) {
    g.b.c = new C();
  }

  public class A {
    B b = new B();
  }

  public class B {
    C c;
  }

  public class C implements AllocatedObject {
    String g;
  }
}
