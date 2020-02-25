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
package test.cases.sets;

import java.util.HashSet;
import org.junit.Ignore;
import org.junit.Test;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class HashSetsLongTest extends AbstractBoomerangTest {
  @Ignore
  @Test
  public void addAndRetrieve() {
    HashSet<Object> set = new HashSet<>();
    AllocatedObject alias = new AllocatedObject() {};
    AllocatedObject alias3 = new AllocatedObject() {};
    set.add(alias);
    set.add(alias3);
    Object alias2 = null;
    for (Object o : set) alias2 = o;
    Object ir = alias2;
    Object query2 = ir;
    queryFor(query2);
  }

  @Override
  protected boolean includeJDK() {
    return true;
  }
}
