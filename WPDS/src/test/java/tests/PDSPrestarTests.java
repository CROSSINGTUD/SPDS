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
package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static tests.TestHelper.ACC;
import static tests.TestHelper.a;
import static tests.TestHelper.accepts;
import static tests.TestHelper.normal;
import static tests.TestHelper.pop;
import static tests.TestHelper.push;
import static tests.TestHelper.t;

import java.util.Collection;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import tests.TestHelper.Abstraction;
import tests.TestHelper.StackSymbol;
import wpds.impl.PAutomaton;
import wpds.impl.PushdownSystem;
import wpds.impl.Transition;

@Ignore
public class PDSPrestarTests {

  private PushdownSystem<StackSymbol, Abstraction> pds;

  @Before
  public void init() {
    pds = new PushdownSystem<StackSymbol, Abstraction>() {};
  }

  @Test
  public void simple() {
    pds.addRule(normal(1, "1", 1, "2"));
    pds.addRule(normal(1, "2", 1, "3"));
    PAutomaton<StackSymbol, Abstraction> fa = accepts(1, "3");
    pds.prestar(fa);
    assertEquals(fa.getTransitions().size(), 3);
    assertEquals(fa.getStates().size(), 2);
    assertTrue(fa.getStates().contains(a(1)));
  }

  @Test
  public void simple2() {
    pds.addRule(normal(1, "a", 2, "b"));
    pds.addRule(normal(2, "b", 2, "c"));
    PAutomaton<StackSymbol, Abstraction> fa = accepts(2, "c");
    pds.prestar(fa);
    assertEquals(fa.getTransitions().size(), 3);
    assertEquals(fa.getStates().size(), 3);
    assertTrue(fa.getStates().contains(a(1)));
    assertTrue(fa.getStates().contains(a(2)));
  }

  @Test
  public void pushTest() {
    pds.addRule(normal(1, "a", 1, "b"));
    pds.addRule(push(1, "b", 1, "c", "d"));
    pds.addRule(pop(1, "c", 1));
    pds.addRule(normal(1, "d", 1, "e"));
    PAutomaton<StackSymbol, Abstraction> fa = accepts(1, "e");
    pds.prestar(fa);
    assertTrue(fa.getTransitions().contains(t(1, "c", 1)));
    assertTrue(fa.getTransitions().contains(t(1, "a", ACC)));
  }

  @Test
  public void doublePushTest() {
    pds.addRule(normal(1, "a", 1, "b"));
    pds.addRule(normal(1, "b", 1, "c"));
    pds.addRule(push(1, "c", 1, "d", "e"));
    pds.addRule(push(1, "d", 1, "h", "i"));
    pds.addRule(pop(1, "h", 1));
    pds.addRule(pop(1, "d", 1));
    pds.addRule(normal(1, "e", 1, "k"));
    PAutomaton<StackSymbol, Abstraction> fa = accepts(1, "k");
    pds.prestar(fa);
    System.out.println(fa);
    assertTrue(fa.getTransitions().contains(t(1, "k", ACC)));
    fa = accepts(1, "k");
    pds.prestar(fa);
    assertTrue(fa.getTransitions().contains(t(1, "a", ACC)));
  }

  @Test
  public void recPushTest() {
    pds.addRule(normal(1, "a", 1, "b"));
    pds.addRule(normal(1, "b", 1, "c"));
    pds.addRule(push(1, "c", 1, "d", "e"));
    pds.addRule(normal(1, "d", 1, "f"));
    pds.addRule(push(1, "f", 1, "d", "h"));
    pds.addRule(pop(1, "d", 1));
    pds.addRule(normal(1, "e", 1, "k"));
    PAutomaton<StackSymbol, Abstraction> fa = accepts(1, "k");
    pds.prestar(fa);
    System.out.println(fa);
    assertTrue(fa.getTransitions().contains(t(1, "c", ACC)));
    assertTrue(fa.getTransitions().contains(t(1, "a", ACC)));
  }

  @Test
  public void recPushTestSimple() {
    pds.addRule(push(1, "a", 1, "d", "e"));
    pds.addRule(push(1, "d", 1, "d", "h"));
    pds.addRule(pop(1, "d", 1));
    pds.addRule(normal(1, "e", 1, "k"));
    PAutomaton<StackSymbol, Abstraction> fa = accepts(1, "k");
    pds.prestar(fa);
    assertTrue(fa.getTransitions().contains(t(1, "a", ACC)));
  }

  // Example taken from http://research.cs.wisc.edu/wpis/papers/fsttcs07.invited.pdf
  @Test
  public void paperEx() {
    pds.addRule(normal(1, "n1", 1, "n2"));
    pds.addRule(normal(1, "n1", 1, "n3"));
    pds.addRule(push(1, "n2", 1, "n7", "n4"));
    pds.addRule(push(1, "n3", 1, "n7", "n5"));
    pds.addRule(normal(1, "n4", 1, "n6"));
    pds.addRule(normal(1, "n5", 1, "n6"));
    pds.addRule(normal(1, "n7", 1, "n8"));
    pds.addRule(pop(1, "n8", 1));
    PAutomaton<StackSymbol, Abstraction> fa = accepts(1, "n6");
    pds.prestar(fa);
    System.out.println(fa);
    Collection<Transition<StackSymbol, Abstraction>> transitions = fa.getTransitions();
    transitions.remove(t(1, "n1", ACC));
    transitions.remove(t(1, "n2", ACC));
    transitions.remove(t(1, "n3", ACC));
    transitions.remove(t(1, "n4", ACC));
    transitions.remove(t(1, "n5", ACC));
    transitions.remove(t(1, "n6", ACC));
    transitions.remove(t(1, "n7", 1));
    transitions.remove(t(1, "n8", 1));
    assertTrue(transitions.isEmpty());
  }
}
