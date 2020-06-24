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

public class ReadTwiceSameFieldTest extends AbstractBoomerangTest {
  @Test
  public void recursiveTest() {
    Container a = new Container();
    Container c = a.d;
    Container alias = c.d;
    queryFor(alias);
  }

  @Test
  public void readFieldTwice() {
    Container a = new Container();
    Container c = a.d;
    Container alias = c.d;
    queryFor(alias);
  }

  private class Container {
    Container d;

    Container() {
      if (staticallyUnknown()) d = new Alloc();
      else d = null;
    }
  }

  private class DeterministicContainer {
    DeterministicContainer d;

    DeterministicContainer() {
      d = new DeterministicAlloc();
    }
  }

  private class DeterministicAlloc extends DeterministicContainer implements AllocatedObject {}

  private class Alloc extends Container implements AllocatedObject {}
}
