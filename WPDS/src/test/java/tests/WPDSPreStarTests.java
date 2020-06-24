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
import static tests.TestHelper.t;
import static tests.TestHelper.waccepts;
import static tests.TestHelper.wnormal;
import static tests.TestHelper.wpop;
import static tests.TestHelper.wpush;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import tests.TestHelper.Abstraction;
import tests.TestHelper.StackSymbol;
import wpds.impl.WeightedPAutomaton;
import wpds.impl.WeightedPushdownSystem;

@Ignore
public class WPDSPreStarTests {
  private WeightedPushdownSystem<StackSymbol, Abstraction, NumWeight> pds;

  @Before
  public void init() {
    pds = new WeightedPushdownSystem<StackSymbol, Abstraction, NumWeight>();
  }

  @Test
  public void simple() {
    pds.addRule(wnormal(1, "a", 2, "b", w(2)));
    pds.addRule(wnormal(2, "b", 3, "c", w(3)));
    WeightedPAutomaton<StackSymbol, Abstraction, NumWeight> fa = waccepts(3, "c", w(0));
    pds.prestar(fa);
    assertEquals(fa.getTransitions().size(), 3);
    assertEquals(fa.getStates().size(), 4);
    assertEquals(fa.getWeightFor(t(1, "a", ACC)), w(5));
  }

  @Test
  public void branch() {
    pds.addRule(wnormal(1, "a", 1, "b", w(2)));
    pds.addRule(wnormal(1, "b", 1, "c", w(4)));
    pds.addRule(wnormal(1, "a", 1, "d", w(3)));
    pds.addRule(wnormal(1, "d", 1, "c", w(3)));
    WeightedPAutomaton<StackSymbol, Abstraction, NumWeight> fa = waccepts(1, "c", w(0));
    pds.prestar(fa);
    assertEquals(fa.getWeightFor(t(1, "a", ACC)), w(6));
    assertEquals(fa.getWeightFor(t(1, "b", ACC)), w(4));
    assertEquals(fa.getWeightFor(t(1, "d", ACC)), w(3));
  }

  @Test
  public void push1() {
    pds.addRule(wnormal(1, "a", 1, "b", w(2)));
    pds.addRule(wpush(1, "b", 1, "c", "d", w(3)));
    pds.addRule(wnormal(1, "c", 1, "e", w(1)));
    pds.addRule(wpop(1, "e", 1, w(5)));
    pds.addRule(wnormal(1, "d", 1, "f", w(6)));
    WeightedPAutomaton<StackSymbol, Abstraction, NumWeight> fa = waccepts(1, "f", w(0));
    pds.prestar(fa);
    assertEquals(fa.getWeightFor(t(1, "a", ACC)), w(17));
    assertEquals(fa.getWeightFor(t(1, "b", ACC)), w(15));
    assertEquals(fa.getWeightFor(t(1, "c", 1)), w(6));
  }

  private static NumWeight w(int i) {
    return new NumWeight(i);
  }
}
