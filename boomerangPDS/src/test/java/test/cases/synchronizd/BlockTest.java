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
package test.cases.synchronizd;

import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class BlockTest extends AbstractBoomerangTest {

  private Object field;

  @Test
  public void block() {
    synchronized (field) {
      AllocatedObject o = new Alloc();
      queryFor(o);
    }
  }

  @Test
  public void block2() {
    set();
    synchronized (field) {
      Object o = field;
      queryFor(o);
    }
  }

  private void set() {
    synchronized (field) {
      field = new Alloc();
    }
  }
}
