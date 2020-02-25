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

public class ReturnPOITest extends AbstractBoomerangTest {
  public class A {
    B b;
  }

  public class B {
    C c;
  }

  public class C implements AllocatedObject {}

  @Test
  public void indirectAllocationSite() {
    B a = new B();
    B e = a;
    allocation(a);
    C alias = e.c;
    C query = a.c;
    queryFor(query);
  }

  private void allocation(B a) {
    C d = new C();
    a.c = d;
  }

  @Test
  public void unbalancedReturnPOI1() {
    C a = new C();
    B b = new B();
    B c = b;
    setField(b, a);
    C alias = c.c;
    queryFor(a);
  }

  private void setField(B a2, C a) {
    a2.c = a;
  }

  @Test
  public void unbalancedReturnPOI3() {
    B b = new B();
    B c = b;
    setField(c);
    C query = c.c;
    queryFor(query);
  }

  private void setField(B c) {
    c.c = new C();
  }

  @Test
  public void whyRecursiveReturnPOIIsNecessary() {
    C c = new C();
    B b = new B();
    A a = new A();
    A a2 = a;
    a2.b = b;
    B b2 = b;
    setFieldTwo(a, c);
    C alias = a2.b.c;
    queryFor(c);
  }

  @Test
  public void whysRecursiveReturnPOIIsNecessary() {
    C c = new C();
    B b = new B();
    A a = new A();
    A a2 = a;
    a2.b = b;
    B b2 = b;
    setFieldTwo(a, c);
    C alias = a2.b.c;
    queryFor(alias);
  }

  private void setFieldTwo(A b, C a) {
    b.b.c = a;
  }

  @Test
  public void whysRecursiveReturnPOIIsNecessary3Addressed() {
    C x = new C();
    B y = new B();
    A z = new A();
    A aliasOuter = z;
    aliasOuter.b = y;
    setFieldTwo3Addresses(z, x);
    B l1 = aliasOuter.b;
    C alias = l1.c;
    queryFor(alias);
  }

  private void setFieldTwo3Addresses(A base, C overwrite) {
    B loaded = base.b;
    loaded.c = overwrite;
  }
}
