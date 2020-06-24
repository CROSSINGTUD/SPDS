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

public class IntraproceduralStrongUpdateTest extends AbstractBoomerangTest {
  @Test
  public void strongUpdateWithField() {
    A a = new A();
    a.field = new Object();
    A b = a;
    b.field = new AllocatedObject() {};
    Object alias = a.field;
    queryFor(alias);
  }

  @Test
  public void strongUpdateWithFieldSwapped() {
    A a = new A();
    A b = a;
    b.field = new Object();
    a.field = new AllocatedObject() {};
    Object alias = a.field;
    queryFor(alias);
  }

  private class A {
    Object field;
  }

  @Test
  public void innerClass() {
    A a = new A();
    A b = a;
    b.field = new I();
    Object alias = a.field;
    queryFor(alias);
  }

  private class I implements AllocatedObject {}

  private static class B {
    Object field;
  }

  @Test
  public void anonymousClass() {
    B a = new B();
    B b = a;
    b.field = new AllocatedObject() {};
    Object alias = a.field;
    queryFor(alias);
  }
}
