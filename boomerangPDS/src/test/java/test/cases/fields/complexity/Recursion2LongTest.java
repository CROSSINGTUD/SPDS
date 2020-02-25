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
package test.cases.fields.complexity;

import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class Recursion2LongTest extends AbstractBoomerangTest {
  @Test
  public void test() {
    Alloc alloc = new Alloc();
    Alloc alias = main(alloc, new A());
    queryFor(alias);
  }

  public static interface IFoo {
    DS before(DS ds);

    DS after(DS ds);
  }

  private static class DS {
    public DS a;
    private Alloc result;
  }

  public Alloc main(Alloc object, IFoo foo) {
    DS ds = new DS();
    ds.result = object;
    DS a = foo.before(ds);
    DS b = foo.after(a);
    ds = b;
    // ds = foo.before(ds);
    // ds = foo.after(ds);
    // ds = foo.before(ds);
    // ds = foo.after(ds); // ...
    return ds.result;
  }

  public static class A implements IFoo {
    public IFoo foo;

    public DS before(DS ds) {
      if (random()) ds = ds.a;
      if (random()) ds.a = ds;
      if (random()) ds.a = null;
      if (random()) ds = foo.before(ds);
      return ds;
    }

    public DS after(DS ds) {
      DS ret = ds;
      if (random()) ret = foo.after(ds);
      if (random()) ret = ds.a;
      if (random()) ret.a = ds;
      if (random()) ret.a = null;
      return ret;
    }
  }

  public static boolean random() {
    return true;
  }
}
