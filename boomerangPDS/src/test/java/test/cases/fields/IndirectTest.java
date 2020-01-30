package test.cases.fields;

/**
 * *****************************************************************************
 * Copyright (c) 2018
 * University, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */

import org.junit.Test;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class IndirectTest extends AbstractBoomerangTest {
  public class A implements AllocatedObject {
    B b;
    A() {
      b = new B();
    }
  }

  public class B {
    String t;
    public B(){
      t = new String();
    }
  }


  @Test
  public void simpleButDiffer() {
    A x = new A();
    x.b.t = someString();
    foo(x);
    queryFor(x);
  }

  private <T> void foo(T x) {

  }

  private String someString() {
    return new String();
  }
}
