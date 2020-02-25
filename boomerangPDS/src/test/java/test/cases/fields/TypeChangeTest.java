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

public class TypeChangeTest extends AbstractBoomerangTest {
  @Test
  public void returnValue() {
    D f = new D();
    Object amIThere = f.getField();
    queryFor(amIThere);
  }

  @Test
  public void doubleReturnValue() {
    D f = new D();
    Object t = f.getDoubleField();
    queryFor(t);
  }

  @Test
  public void returnValueAndBackCast() {
    D f = new D();
    Object t = f.getField();
    AllocatedObject u = (AllocatedObject) t;
    queryFor(u);
  }

  public static class D {
    Alloc f = new Alloc();
    D d = new D();

    public Object getField() {
      Alloc varShouldBeThere = this.f;
      return varShouldBeThere;
    }

    public Object getDoubleField() {
      return d.getField();
    }
  }
}
