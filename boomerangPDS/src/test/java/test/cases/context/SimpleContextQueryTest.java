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
package test.cases.context;

import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class SimpleContextQueryTest extends AbstractBoomerangTest {
  @Test
  public void outerAllocation() {
    AllocatedObject alloc = new Alloc();
    methodOfQuery(alloc);
  }

  private void methodOfQuery(AllocatedObject allocInner) {
    AllocatedObject alias = allocInner;
    queryFor(alias);
  }

  @Test
  public void outerAllocation2() {
    AllocatedObject alloc = new AllocatedObject() {};
    AllocatedObject same = alloc;
    methodOfQuery(alloc, same);
  }

  @Test
  public void outerAllocation3() {
    AllocatedObject alloc = new AllocatedObject() {};
    Object same = new Object();
    methodOfQuery(alloc, same);
  }

  private void methodOfQuery(Object alloc, Object alias) {
    queryFor(alloc);
  }
}
