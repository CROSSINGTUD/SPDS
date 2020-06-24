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
import java.util.Set;
import java.util.TreeSet;
import org.junit.Ignore;
import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class TreeSetsLongTest extends AbstractBoomerangTest {
  @Ignore
  @Test
  public void addAndRetrieve() {
    Set<Object> set = new TreeSet<Object>();
    Alloc alias = new Alloc();
    set.add(alias);
    alias = new Alloc();
    set.add(alias);

    Object alias2 = null;
    for (Object o : set) alias2 = o;
    Object ir = alias2;
    Object query2 = ir;
    Set<Object> set2 = new TreeSet<Object>();
    Object alias1 = new Object();
    set2.add(alias1);
    alias1 = new Object();
    set2.add(alias1);
    alias1 = new Object();
    queryFor(query2);
    otherMap();
    otherMap2();
    hashMap();
  }

  private void hashMap() {
    HashSet<Object> map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
    map = new HashSet<Object>();
    map.add(new Object());
  }

  private void otherMap2() {
    Set<Object> set = new TreeSet<Object>();
    Object alias = new Object();
    set.add(alias);
    alias = new Object();
    set.add(alias);
    alias = new Object();
    set.add(alias);
    alias = new Object();
    set.add(alias);
    alias = new Object();
    set.add(alias);

    Object alias2 = null;
    for (Object o : set) alias2 = o;
  }

  private void otherMap() {
    Set<Object> set = new TreeSet<Object>();
    Object alias = new Object();
    set.add(alias);
    alias = new Object();
    set.add(alias);
    alias = new Object();
    set.add(alias);
    alias = new Object();
    set.add(alias);
    alias = new Object();
    set.add(alias);

    Object alias2 = null;
    for (Object o : set) alias2 = o;
  }

  @Override
  protected boolean includeJDK() {
    return true;
  }
}
