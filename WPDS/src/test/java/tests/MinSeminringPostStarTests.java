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
import static tests.TestHelper.ACC;
import static tests.TestHelper.a;
import static tests.TestHelper.s;
import static tests.TestHelper.t;

import org.junit.Before;
import org.junit.Test;
import tests.TestHelper.Abstraction;
import tests.TestHelper.StackSymbol;
import wpds.impl.NormalRule;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.impl.WeightedPAutomaton;
import wpds.impl.WeightedPushdownSystem;

public class MinSeminringPostStarTests {
  private WeightedPushdownSystem<StackSymbol, Abstraction, MinSemiring> pds;

  @Before
  public void init() {
    pds = new WeightedPushdownSystem<StackSymbol, Abstraction, MinSemiring>();
  }

  @Test
  public void simple() {
    pds.addRule(wnormal(1, "a", 2, "b", w(1)));
    pds.addRule(wnormal(2, "b", 3, "c", w(1)));
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring> fa = waccepts(1, "a", w(0));
    pds.poststar(fa);
    assertEquals(fa.getTransitions().size(), 3);
    assertEquals(fa.getStates().size(), 4);
    assertEquals(w(2), fa.getWeightFor(t(3, "c", ACC)));
  }

  @Test
  public void branch() {
    pds.addRule(wnormal(1, "a", 1, "b", w(1)));
    pds.addRule(wnormal(1, "b", 1, "c", w(1)));
    pds.addRule(wnormal(1, "a", 1, "d", w(1)));
    pds.addRule(wnormal(1, "d", 1, "c", w(1)));
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring> fa = waccepts(1, "a", w(0));
    pds.poststar(fa);
    assertEquals(w(2), fa.getWeightFor(t(1, "c", ACC)));
    assertEquals(w(1), fa.getWeightFor(t(1, "b", ACC)));
    assertEquals(w(1), fa.getWeightFor(t(1, "d", ACC)));
  }

  @Test
  public void push1() {
    pds.addRule(wnormal(1, "a", 1, "b", w(1)));
    pds.addRule(wpush(1, "b", 1, "c", "d", w(1)));
    pds.addRule(wnormal(1, "c", 1, "e", w(1)));
    pds.addRule(wpop(1, "e", 1, w(1)));
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring> fa = waccepts(1, "a", w(0));
    pds.poststar(fa);
    assertEquals(w(1), fa.getWeightFor(t(1, "b", ACC)));
    assertEquals(w(4), fa.getWeightFor(t(1, "d", ACC)));
    // assertEquals(w(2), fa.getWeightFor(t(1, "e", a(1, "c"))));
  }

  @Test
  public void push2() {
    pds.addRule(wnormal(1, "a", 2, "b", w(1)));
    pds.addRule(wpush(2, "b", 3, "c", "d", w(2)));
    pds.addRule(wnormal(3, "c", 4, "e", w(1)));
    pds.addRule(wpop(4, "e", 5, w(1)));
    pds.addRule(wnormal(5, "d", 2, "f", w(10)));
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring> fa = waccepts(1, "a", w(0));

    pds.poststar(fa);
    assertEquals(w(5), fa.getWeightFor(t(5, "d", ACC)));
    assertEquals(w(15), fa.getWeightFor(t(2, "f", ACC)));
  }

  @Test
  public void twoCall() {
    pds.addRule(wnormal(1, "a", 1, "b", w(1)));
    pds.addRule(wpush(1, "b", 1, "call", "d", w(1)));
    pds.addRule(wnormal(1, "call", 1, "e", w(1)));
    pds.addRule(wpop(1, "e", 1, w(1)));
    pds.addRule(wnormal(1, "d", 1, "f", w(1)));
    pds.addRule(wpush(1, "f", 1, "call", "g", w(1)));
    pds.addRule(wnormal(1, "g", 1, "h", w(1)));
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring> fa = waccepts(1, "a", w(0));
    pds.poststar(fa);
    assertEquals(w(9), fa.getWeightFor(t(1, "h", ACC)));
  }

  private static MinSemiring w(int i) {
    return new MinSemiring(i);
  }

  static WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring> waccepts(
      int a, String c, MinSemiring weight) {
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring> aut =
        new WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring>() {

          @Override
          public Abstraction createState(Abstraction d, StackSymbol loc) {
            return new Abstraction(d, loc);
          }

          @Override
          public StackSymbol epsilon() {
            return s("EPS");
          }

          @Override
          public MinSemiring getOne() {
            return MinSemiring.one();
          }

          @Override
          public boolean isGeneratedState(Abstraction d) {
            return d.s != null;
          }
        };
    aut.addFinalState(ACC);
    aut.addTransition(t(a, c, ACC));
    aut.addWeightForTransition(t(a, c, ACC), weight);
    return aut;
  }

  static NormalRule<StackSymbol, Abstraction, MinSemiring> wnormal(
      int a, String n, int b, String m, MinSemiring w) {
    return new NormalRule<StackSymbol, Abstraction, MinSemiring>(a(a), s(n), a(b), s(m), w);
  }

  static PushRule<StackSymbol, Abstraction, MinSemiring> wpush(
      int a, String n, int b, String m, String l, MinSemiring w) {
    return new PushRule<>(a(a), s(n), a(b), s(m), s(l), w);
  }

  static PopRule<StackSymbol, Abstraction, MinSemiring> wpop(
      int a, String n, int b, MinSemiring w) {
    return new PopRule<StackSymbol, Abstraction, MinSemiring>(a(a), s(n), a(b), w);
  }
}
