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
package test.cases.context;

import org.junit.Test;
import test.cases.basic.Allocation;
import test.core.AbstractBoomerangTest;

public class PathingContextProblemTest extends AbstractBoomerangTest {
  @Test
  public void start() {
    Inner i = new Inner();
    i.test1();
    i.test2();
  }

  public static class Inner {

    public void callee(Object a, Object b) {
      queryFor(a);
    }

    public void test1() {
      Object a1 = new Allocation();
      Object b1 = a1;
      callee(a1, b1);
    }

    public void test2() {
      Object a2 = new Allocation();
      Object b2 = new Object();
      callee(a2, b2);
    }
  }

  @Test
  public void start2() {
    Inner i = new Inner();
    i.test1();
    i.test2();
  }

  public static class Inner2 {

    public void callee(Object a, Object b) {
      queryFor(b);
    }

    public void test1() {
      Object a1 = new Allocation();
      Object b1 = a1;
      callee(a1, b1);
    }

    public void test2() {
      Object a2 = new Object();
      Object b2 = new Allocation();
      callee(a2, b2);
    }
  }
}
