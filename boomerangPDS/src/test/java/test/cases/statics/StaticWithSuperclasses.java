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

public class StaticWithSuperclasses extends AbstractBoomerangTest {
  @Test
  public void simple() {
    List list = new List();
    Object o = list.get();
    queryForAndNotEmpty(o);
  }

  private static class List {

    private static Object elementData = new Alloc();

    public Object get() {
      return elementData;
    }
  }

  @Test
  public void supclass() {
    MyList list = new MyList();
    Object o = list.get();
    queryForAndNotEmpty(o);
  }

  private static class MyList extends List {

    private static Object elementData2 = new Alloc();

    public Object get() {
      return elementData2;
    }
  }
}
