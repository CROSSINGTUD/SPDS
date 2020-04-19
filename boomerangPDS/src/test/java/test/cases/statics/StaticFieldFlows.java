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
package test.cases.statics;

import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class StaticFieldFlows extends AbstractBoomerangTest {
  private static Object alloc;
  private static Alloc instance;
  private static Alloc i;

  @Test
  public void simple() {
    alloc = new Alloc();
    Object alias = alloc;
    queryFor(alias);
  }

  @Test
  public void simple2() {
    alloc = new Alloc();
    Object sr = new Object();
    Object r = new String();
    queryFor(alloc);
  }

  @Test
  public void withCallInbetween() {
    alloc = new Alloc();
    alloc.toString();
    foo();
    queryFor(alloc);
  }

  private void foo() {}

  @Test
  public void singleton() {
    Alloc singleton = StaticFieldFlows.v();
    Object alias = singleton;
    queryFor(alias);
  }

  @Test
  public void getAndSet() {
    setStatic();
    Object alias = getStatic();
    queryFor(alias);
  }

  private Object getStatic() {
    return i;
  }

  private void setStatic() {
    i = new Alloc();
  }

  @Test
  public void doubleUnbalancedSingleton() {
    Alloc singleton = returns();
    Object alias = singleton;
    queryFor(alias);
  }

  private static Alloc returns() {
    return StaticFieldFlows.v();
  }

  private static Alloc v() {
    if (instance == null) instance = new Alloc();
    Alloc loaded = instance;
    return loaded;
  }

  @Test
  public void overwriteStatic() {
    alloc = new Object();
    alloc = new Alloc();
    Object alias = alloc;
    queryFor(alias);
  }

  @Test
  public void overwriteStaticInter() {
    alloc = new Object();
    update();
    irrelevantFlow();
    Object alias = alloc;
    queryFor(alias);
  }

  private int irrelevantFlow() {
    int x = 1;
    x = 2;
    x = 3;
    x = 4;
    return x;
  }

  private void update() {
    alloc = new Alloc();
  }

  @Test
  public void intraprocedural() {
    setStaticField();
    Object alias = alloc;
    queryFor(alias);
  }

  private void setStaticField() {
    alloc = new Alloc();
  }
}
