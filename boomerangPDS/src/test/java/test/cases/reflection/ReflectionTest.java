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
package test.cases.reflection;

import org.junit.Ignore;
import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class ReflectionTest extends AbstractBoomerangTest {

  @Test
  public void bypassClassForName()
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    Alloc query = new Alloc();
    Class<?> cls = Class.forName(A.class.getName());
    queryFor(query);
  }

  @Ignore
  @Test
  public void loadObject()
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    Class<?> cls = Class.forName(A.class.getName());
    Object newInstance = cls.newInstance();
    A a = (A) newInstance;
    Alloc query = a.field;
    queryFor(query);
  }

  private static class A {
    Alloc field = new Alloc();
  }
}
