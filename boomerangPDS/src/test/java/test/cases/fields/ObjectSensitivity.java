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

public class ObjectSensitivity extends AbstractBoomerangTest {

  @Test
  public void objectSensitivity0() {
    B b1 = new B();
    Alloc b2 = new Alloc();

    A a1 = new A();
    A a2 = new A();

    a1.f = b1;
    a2.f = b2;
    Object b3 = a1.getF();
    int x = 1;
    Object b4 = a2.getF();
    // flow(b4);
    queryFor(b4);
  }

  @Test
  public void objectSensitivity1() {
    B b1 = new B();
    Alloc b2 = new Alloc();

    A a1 = new A(b1);
    A a2 = new A(b2);

    Object b3 = a1.getF();
    Object b4 = a2.getF();
    // flow(b4);
    queryFor(b4);
  }

  private void flow(Object b3) {
    // TODO Auto-generated method stub

  }

  @Test
  public void objectSensitivity2() {
    Alloc b2 = new Alloc();
    A a2 = new A(b2);

    otherScope();
    Object b4 = a2.getF();

    queryFor(b4);
  }

  private void otherScope() {
    B b1 = new B();
    A a1 = new A(b1);
    Object b3 = a1.getF();
  }

  public static class A {

    public Object f;

    public A(Object o) {
      this.f = o;
    }

    public A() {}

    public void setF(Object b2) {
      this.f = b2;
    }

    public Object getF() {
      return this.f;
    }
  }
}
