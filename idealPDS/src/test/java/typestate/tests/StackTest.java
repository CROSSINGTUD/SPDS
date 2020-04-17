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
import java.util.Stack;
import org.junit.Ignore;
import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.VectorStateMachine;

@SuppressWarnings("deprecation")
public class StackTest extends IDEALTestingFramework {

  @Ignore("Broken since refactoring scope")
  @Test
  public void test1() {
    Stack s = new Stack();
    if (staticallyUnknown()) s.peek();
    else {
      Stack r = s;
      r.pop();
      mustBeInErrorState(r);
    }
    mustBeInErrorState(s);
  }

  @Test
  public void test4simple() {
    Stack s = new Stack();
    s.peek();
    mustBeInErrorState(s);
    s.pop();
    mustBeInErrorState(s);
  }

  @Test
  public void test2() {
    Stack s = new Stack();
    s.add(new Object());
    if (staticallyUnknown()) s.peek();
    else s.pop();
    mustBeInAcceptingState(s);
  }

  @Test
  public void test6() {
    ArrayList l = new ArrayList();
    Stack s = new Stack();
    if (staticallyUnknown()) {
      s.push(new Object());
    }
    if (staticallyUnknown()) {
      s.push(new Object());
    }
    if (!s.isEmpty()) {
      Object pop = s.pop();
      mayBeInErrorState(s);
    }
  }

  @Test
  public void test3() {
    Stack s = new Stack();
    s.peek();
    mustBeInErrorState(s);
    s.pop();
    mustBeInErrorState(s);
  }

  @Test
  public void test5() {
    Stack s = new Stack();
    s.peek();
    mustBeInErrorState(s);
  }

  @Test
  public void test4() {
    Stack s = new Stack();
    s.peek();
    s.pop();

    Stack c = new Stack();
    c.add(new Object());
    c.peek();
    c.pop();
    mustBeInErrorState(s);
    mustBeInAcceptingState(c);
  }

  @Ignore("Broken since refactoring scope")
  @Test
  public void testInNewObject() {
    OwithStack owithStack = new OwithStack();
    owithStack.pushStack(new Object());
    owithStack.get();
    mustBeInAcceptingState(owithStack.stack);
  }

  private static class OwithStack {
    Stack stack;

    public void pushStack(Object o) {
      if (this.stack == null) {
        this.stack = new Stack();
      }
      this.stack.push(o);
    }

    public Object get() {
      if (stack == null || stack.empty()) {
        return null;
      }
      Object peek = this.stack.peek();
      mustBeInAcceptingState(this.stack);
      return peek;
    }
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new VectorStateMachine();
  }
}
