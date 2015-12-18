package tests;

import static org.junit.Assert.assertEquals;
import static tests.TestHelper.ACC;
import static tests.TestHelper.a;
import static tests.TestHelper.t;
import static tests.TestHelper.waccepts;
import static tests.TestHelper.wnormal;
import static tests.TestHelper.wpop;
import static tests.TestHelper.wpush;

import org.junit.Before;
import org.junit.Test;

import tests.TestHelper.Abstraction;
import tests.TestHelper.StackSymbol;
import wpds.impl.WeightedPAutomaton;
import wpds.impl.WeightedPushdownSystem;

public class WPDSPostStarTests {
  private WeightedPushdownSystem<StackSymbol, Abstraction, NumWeight<StackSymbol>> pds;

  @Before
  public void init() {
    pds = new WeightedPushdownSystem<StackSymbol, Abstraction, NumWeight<StackSymbol>>() {

      @Override
      public NumWeight<StackSymbol> getZero() {
        return NumWeight.zero();
      }

      @Override
      public NumWeight<StackSymbol> getOne() {
        return NumWeight.one();
      }
    };
  }

  @Test
  public void simple() {
    pds.addRule(wnormal(1, "a", 2, "b", w(2)));
    pds.addRule(wnormal(2, "b", 3, "c", w(3)));
    WeightedPAutomaton<StackSymbol, Abstraction, NumWeight<StackSymbol>> fa =
        waccepts(1, "a", w(0));
    pds.poststar(fa);
    assertEquals(fa.getTransitions().size(), 3);
    assertEquals(fa.getStates().size(), 4);
    assertEquals(fa.getWeightFor(t(3, "c", ACC)), w(5));
  }

  @Test
  public void branch() {
    pds.addRule(wnormal(1, "a", 1, "b", w(2)));
    pds.addRule(wnormal(1, "b", 1, "c", w(3)));
    pds.addRule(wnormal(1, "a", 1, "d", w(3)));
    pds.addRule(wnormal(1, "d", 1, "c", w(3)));
    WeightedPAutomaton<StackSymbol, Abstraction, NumWeight<StackSymbol>> fa =
        waccepts(1, "a", w(0));
    pds.poststar(fa);
    assertEquals(fa.getWeightFor(t(1, "c", ACC)), NumWeight.zero());
    assertEquals(fa.getWeightFor(t(1, "b", ACC)), w(2));
    assertEquals(fa.getWeightFor(t(1, "d", ACC)), w(3));
  }

  @Test
  public void push1() {
    pds.addRule(wnormal(1, "a", 1, "b", w(2)));
    pds.addRule(wpush(1, "b", 1, "c", "d", w(3)));
    pds.addRule(wnormal(1, "c", 1, "e", w(1)));
    pds.addRule(wpop(1, "e", 1, w(5)));
    WeightedPAutomaton<StackSymbol, Abstraction, NumWeight<StackSymbol>> fa =
        waccepts(1, "a", w(0));
    pds.poststar(fa);
    assertEquals(fa.getWeightFor(t(1, "b", ACC)), w(2));
    assertEquals(fa.getWeightFor(t(1, "c", a(1, "c"))), w(5));
    assertEquals(fa.getWeightFor(t(1, "e", a(1, "c"))), w(6));
  }

  @Test
  public void push2() {
    pds.addRule(wnormal(1, "a", 2, "b", w(2)));
    pds.addRule(wpush(2, "b", 3, "c", "d", w(3)));
    pds.addRule(wnormal(3, "c", 4, "e", w(1)));
    pds.addRule(wpop(4, "e", 5, w(5)));
    WeightedPAutomaton<StackSymbol, Abstraction, NumWeight<StackSymbol>> fa =
        waccepts(1, "a", w(0));
    pds.poststar(fa);
    assertEquals(fa.getWeightFor(t(5, "d", ACC)), w(11));
    assertEquals(fa.getWeightFor(t(4, "e", a(3, "c"))), w(6));
  }

  @Test
  public void twoCall() {
    pds.addRule(wnormal(1, "a", 1, "b", w(1)));
    pds.addRule(wpush(1, "b", 2, "call", "d", w(2)));
    pds.addRule(wnormal(2, "call", 2, "e", w(3)));
    pds.addRule(wpop(2, "e", 3, w(4)));
    pds.addRule(wnormal(3, "d", 1, "f", w(5)));
    pds.addRule(wpush(3, "f", 2, "call", "g", w(6)));
    pds.addRule(wnormal(3, "g", 4, "h", w(7)));
    WeightedPAutomaton<StackSymbol, Abstraction, NumWeight<StackSymbol>> fa =
        waccepts(1, "a", w(0));
    pds.poststar(fa);
    System.out.println(fa);
    assertEquals(w(35), fa.getWeightFor(t(1, "h", ACC)));
    assertEquals(w(6), fa.getWeightFor(t(4, "e", a(3, "c"))));
  }

  @Test
  public void twoCallOnlyReturnWeight() {
    pds.addRule(wnormal(1, "a", 1, "b", w(0)));
    pds.addRule(wpush(1, "b", 2, "call", "d", w(0)));
    pds.addRule(wnormal(2, "call", 2, "e", w(0)));
    pds.addRule(wpop(2, "e", 3, w(4)));
    pds.addRule(wnormal(3, "d", 1, "f", w(0)));
    pds.addRule(wpush(3, "f", 2, "call", "g", w(0)));
    pds.addRule(wnormal(3, "g", 4, "h", w(0)));
    WeightedPAutomaton<StackSymbol, Abstraction, NumWeight<StackSymbol>> fa =
        waccepts(1, "a", w(0));
    pds.poststar(fa);
    System.out.println(fa);
    assertEquals(w(35), fa.getWeightFor(t(1, "h", ACC)));
    assertEquals(w(6), fa.getWeightFor(t(4, "e", a(3, "c"))));
  }


  private static NumWeight<StackSymbol> w(int i) {
    return new NumWeight<>(i);
  }
}
