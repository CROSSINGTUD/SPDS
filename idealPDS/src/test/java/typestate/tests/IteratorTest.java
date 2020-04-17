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
package typestate.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.HasNextStateMachine;

public class IteratorTest extends IDEALTestingFramework {

  @Test
  public void test1() {
    List<Object> list = new LinkedList<>();
    list.add(new Object());
    list.add(new Object());
    for (Object l : list) {
      System.out.println(l);
    }
    mustBeInAcceptingState(list.iterator());
  }

  @Test
  public void test2() {
    MyLinkedList<Object> list = new MyLinkedList<>();
    list.add(new Object());
    Iterator<Object> iterator = list.iterator();
    iterator.hasNext();
    iterator.next();
    iterator.next();
    mustBeInErrorState(iterator);
  }

  @Ignore("Fails when Exception analysis is off, requires JimpleBasedInterproceduralICFG(true)")
  @Test
  public void test3() {
    LinkedList<Object> list = new LinkedList<>();
    list.add(new Object());
    Iterator it1 = list.iterator();
    Object each = null;
    for (; it1.hasNext(); each = it1.next()) {
      try {
        each.toString();
      } catch (Throwable e) {
        e.getMessage();
      }
    }
    mustBeInAcceptingState(it1);
  }

  @Test
  public void test4() {
    List l1 = new ArrayList();
    List l2 = new ArrayList();

    l1.add("foo");
    l1.add("moo");
    l1.add("zoo");

    Object v;
    Iterator it1 = l1.iterator();
    for (; it1.hasNext(); v = it1.next()) {
      System.out.println(foo(it1));
    }
    mayBeInErrorState(it1);
  }

  @Test
  public void chartTest() {
    AxisCollection col = new AxisCollection();
    col.add(new Object());
    Iterator iterator = col.getAxesAtBottom().iterator();
    while (iterator.hasNext()) {
      Object next = iterator.next();
      next.hashCode();
    }
    iterator = col.getAxesAtTop().iterator();
    mustBeInAcceptingState(iterator);
    while (iterator.hasNext()) {
      mustBeInAcceptingState(iterator);
      Object next = iterator.next();
      next.hashCode();
      mustBeInAcceptingState(iterator);
    }
    mustBeInAcceptingState(iterator);
  }

  private static class AxisCollection {
    private ArrayList axesAtTop;
    private ArrayList axesAtBottom;

    public AxisCollection() {
      this.axesAtTop = new ArrayList();
      this.axesAtBottom = new ArrayList();
    }

    public void add(Object object) {
      if (1 + 1 == 2) {
        this.axesAtBottom.add(object);
      } else {
        this.axesAtTop.add(object);
      }
    }

    public ArrayList getAxesAtBottom() {
      return axesAtBottom;
    }

    public ArrayList getAxesAtTop() {
      return axesAtTop;
    }
  }

  public Object foo(Iterator it) {
    return it.next();
  }

  private static class MyLinkedList<V> {

    public void add(Object object) {
      // TODO Auto-generated method stub

    }

    public Iterator<V> iterator() {
      return new MyIterator<V>();
    }
  }

  private static class MyIterator<V> implements Iterator<V> {

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public V next() {
      return null;
    }

    @Override
    public void remove() {
      // TODO Auto-generated method stub
    }
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new HasNextStateMachine();
  }
}
