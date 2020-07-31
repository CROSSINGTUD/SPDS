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

import boomerang.BoomerangOptions;
import boomerang.DefaultBoomerangOptions;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class TreeMapMultipleInstancesTest extends AbstractBoomerangTest {
  @Test
  public void addAndRetrieve() {
    Map<Integer, Object> set = new TreeMap<Integer, Object>();
    Alloc alias = new Alloc();
    set.put(1, alias);
    Object query2 = set.get(2);
    queryFor(query2);
    otherMap();
    otherMap2();
    hashMap();
  }

  @Test
  public void contextSensitive() {
    Map<Integer, Object> map = new TreeMap<Integer, Object>();
    Object alias = new Alloc();
    Object ret = addToMap(map, alias);

    Map<Integer, Object> map2 = new TreeMap<Integer, Object>();
    Object noAlias = new Object();
    Object ret2 = addToMap(map2, noAlias);
    System.out.println(ret2);
    queryFor(ret);
  }

  private Object addToMap(Map<Integer, Object> map, Object alias) {
    map.put(1, alias);
    Object query2 = map.get(2);
    return query2;
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
    Map<Integer, Object> set = new TreeMap<Integer, Object>();
    Object alias = new Object();
    set.put(1, alias);
    set.put(2, alias);
    set.get(3);
  }

  private void otherMap() {
    Map<Integer, Object> set = new TreeMap<Integer, Object>();
    Object alias = new Object();
    set.put(1, alias);
    set.put(2, alias);
    set.get(3);
  }

  @Override
  protected boolean includeJDK() {
    return true;
  }

  @Override
  protected BoomerangOptions createBoomerangOptions() {
    return new DefaultBoomerangOptions() {
      @Override
      public boolean handleMaps() {
        return false;
      }

      @Override
      public int analysisTimeoutMS() {
        return analysisTimeout;
      }
    };
  }
}
