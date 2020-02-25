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
package test.cases.lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class ArrayAndLinkedListsTest extends AbstractBoomerangTest {

  @Override
  protected boolean includeJDK() {
    return true;
  }

  @Test
  public void addAndRetrieve() {
    List<Object> list1 = new LinkedList<>();
    Object o = new Alloc();
    add(list1, o);
    Object o2 = new Object();
    List<Object> list2 = new ArrayList<>();
    add(list2, o2);
    queryFor(o);
  }

  private void add(List<Object> list1, Object o) {
    list1.add(o);
  }

  @Override
  protected Collection<String> errorOnVisitMethod() {
    return Collections.singleton("<java.util.ArrayList: boolean add(java.lang.Object)>");
  }
}
