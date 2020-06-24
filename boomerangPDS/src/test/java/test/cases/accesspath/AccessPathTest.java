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
package test.cases.accesspath;

import org.junit.Ignore;
import org.junit.Test;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

@Ignore
public class AccessPathTest extends AbstractBoomerangTest {
  private static class A {
    B b = null;
    A g;
  }

  private static class AllocA extends A implements AllocatedObject {}

  private static class B implements AllocatedObject {
    B c = null;
    B d = null;
    public B b;
  }

  private static class C {
    B b = null;
    A attr = new A();
  }

  @Test
  public void getAllAliases() {
    A a = new A();
    B alloc = new B();
    a.b = alloc;
    accessPathQueryFor(alloc, "a[b]");
  }

  @Test
  public void sameField() {
    AllocA alloc = new AllocA();
    A b = new A();
    A c = new A();
    b.g = alloc;
    c.g = b;
    accessPathQueryFor(alloc, "b[g];c[g,g]");
  }

  @Test
  public void getAllAliasesBranched() {
    A a = new A();
    A b = new A();
    B alloc = new B();
    if (staticallyUnknown()) {
      a.b = alloc;
    } else {
      b.b = alloc;
    }
    accessPathQueryFor(alloc, "a[b];b[b]");
  }

  @Ignore
  @Test
  public void getAllAliasesLooped() {
    A a = new A();
    B alloc = new B();
    a.b = alloc;
    for (int i = 0; i < 10; i++) {
      B d = alloc;
      alloc.c = d;
    }
    accessPathQueryFor(alloc, "a[b];alloc[c]*");
  }

  @Ignore
  @Test
  public void getAllAliasesLoopedComplex() {
    A a = new A();
    B alloc = new B();
    a.b = alloc;
    for (int i = 0; i < 10; i++) {
      B d = alloc;
      if (staticallyUnknown()) alloc.c = d;
      if (staticallyUnknown()) alloc.d = d;
    }
    accessPathQueryFor(alloc, "a[b];alloc[c]*;alloc[d]*;alloc[c,d];alloc[d,c]");
  }

  @Test
  public void simpleIndirect() {
    A a = new A();
    A b = a;
    B alloc = new B();
    a.b = alloc;
    accessPathQueryFor(alloc, "a[b];b[b]");
  }

  @Test
  public void doubleIndirect() {
    C b = new C();
    B alloc = new B();
    b.attr.b = alloc;
    accessPathQueryFor(alloc, "b[attr,b]");
  }

  @Test
  public void contextQuery() {
    B a = new B();
    B b = a;
    context(a, b);
  }

  private void context(B a, B b) {
    accessPathQueryFor(a, "a;b");
  }

  @Test
  public void doubeContextQuery() {
    B a = new B();
    B b = a;
    context1(a, b);
  }

  private void context1(B a, B b) {
    context(a, b);
  }

  static void use(Object b) {}

  @Test
  public void twoLevelTest() {
    C b = new C();
    taintMe(b);
  }

  @Test
  public void threeLevelTest() {
    C b = new C();
    taintOnNextLevel(b);
  }

  private void taintMe(C b) {
    B alloc = new B();
    b.attr.b = alloc;
    accessPathQueryFor(alloc, "alloc;b[attr,b]");
  }

  private void taintOnNextLevel(C b) {
    taintMe(b);
  }

  @Test
  public void hiddenFieldLoad() {
    ClassWithField a = new ClassWithField();
    a.field = new ObjectOfInterest();
    ClassWithField b = a;
    NestedClassWithField n = new NestedClassWithField();
    n.nested = b;
    staticCallOnFile(a, n);
  }

  private static void staticCallOnFile(ClassWithField x, NestedClassWithField n) {
    ObjectOfInterest queryVariable = x.field;
    // The analysis triggers a query for the following variable
    accessPathQueryFor(queryVariable, "x[field];n[nested,field]");
  }

  public static class ClassWithField {
    public ObjectOfInterest field;
  }

  public static class ObjectOfInterest implements AllocatedObject {}

  public static class NestedClassWithField {
    public ClassWithField nested;
  }

  @Test
  public void hiddenFieldLoad2() {
    ObjectOfInterest alloc = new ObjectOfInterest();
    NestedClassWithField n = new NestedClassWithField();
    store(n, alloc);
    accessPathQueryFor(alloc, "n[nested,field]");
  }

  private void store(NestedClassWithField o1, ObjectOfInterest oOfInterest) {
    ClassWithField a = new ClassWithField();
    a.field = oOfInterest;
    ClassWithField b = a;
    o1.nested = b;
  }

  @Test
  public void hiddenFieldLoad3() {
    ObjectOfInterest alloc = new ObjectOfInterest();
    NestedClassWithField n = new NestedClassWithField();
    NestedClassWithField t = n;
    store(n, alloc);
    accessPathQueryFor(alloc, "n[nested,field];t[nested,field]");
    use(t);
  }

  @Test
  public void hiddenFieldLoad4() {
    ObjectOfInterest alloc = new ObjectOfInterest();
    NestedClassWithField n = new NestedClassWithField();
    NestedClassWithField t = n;
    store(n, alloc);
    load(t);
  }

  private void load(NestedClassWithField t) {
    queryFor(t.nested.field);
  }
}
