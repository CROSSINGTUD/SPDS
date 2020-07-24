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
package example;

public class BoomerangExampleTarget1 {
  public static void main(String... args) {
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
    queryFor(queryVariable);
  }

  private static void queryFor(ObjectOfInterest queryVariable) {}

  public static class ClassWithField {
    public ObjectOfInterest field;
  }

  public static class ObjectOfInterest {}

  public static class NestedClassWithField {
    public ClassWithField nested;
  }
}
