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
package boomerang.example;

public class BoomerangExampleTarget2 {
  public static void main(String... args) {
    ClassWithField a = new ClassWithField();
    ClassWithField b = a;
    NestedClassWithField n = new NestedClassWithField();
    n.nested = b;
    staticCallOnFile(n);
  }

  private static void staticCallOnFile(NestedClassWithField n) {
    System.out.println("Will print value 10");
    System.out.println(n.nested.field);
  }

  public static class ClassWithField {
    public int field = 10;
  }

  public static class ObjectOfInterest {}

  public static class NestedClassWithField {
    public ClassWithField nested;
  }
}
