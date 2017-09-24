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
  private WeightedPushdownSystem<StackSymbol, Abstraction, MinSemiring<StackSymbol>> pds;

  @Before
  public void init() {
    pds = new WeightedPushdownSystem<StackSymbol, Abstraction, MinSemiring<StackSymbol>>() {

      @Override
      public MinSemiring<StackSymbol> getZero() {
        return MinSemiring.zero();
      }

      @Override
      public MinSemiring<StackSymbol> getOne() {
        return MinSemiring.one();
      }
    };
  }

  @Test
  public void simple() {
    pds.addRule(wnormal(1, "a", 2, "b", w(1)));
    pds.addRule(wnormal(2, "b", 3, "c", w(1)));
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring<StackSymbol>> fa =
        waccepts(1, "a", w(0));
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
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring<StackSymbol>> fa =
        waccepts(1, "a", w(0));
    pds.poststar(fa);
    System.out.println(fa);
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
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring<StackSymbol>> fa =
        waccepts(1, "a", w(0));
    System.out.println(pds);
    pds.poststar(fa);
    System.out.println(fa);
    assertEquals(w(1), fa.getWeightFor(t(1, "b", ACC)));
    assertEquals(w(4), fa.getWeightFor(t(1, "d", ACC)));
//    assertEquals(w(2), fa.getWeightFor(t(1, "e", a(1, "c"))));
  }

  @Test
  public void push2() {
    pds.addRule(wnormal(1, "a", 2, "b", w(1)));
    pds.addRule(wpush(2, "b", 3, "c", "d", w(2)));
    pds.addRule(wnormal(3, "c", 4, "e", w(1)));
    pds.addRule(wpop(4, "e", 5, w(1)));
    pds.addRule(wnormal(5, "d", 2, "f", w(10)));
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring<StackSymbol>> fa =
        waccepts(1, "a", w(0));

    System.out.println(fa.toDotString());
    System.out.println(fa);
    pds.poststar(fa);
    System.out.println(fa.toDotString());
    System.out.println(fa);
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
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring<StackSymbol>> fa =
        waccepts(1, "a", w(0));
    pds.poststar(fa);
    System.out.println(fa);
    assertEquals(w(9), fa.getWeightFor(t(1, "h", ACC)));
  }

  private static MinSemiring<StackSymbol> w(int i) {
    return new MinSemiring<>(i);
  }



  static WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring<StackSymbol>> waccepts(int a,
      String c, MinSemiring<StackSymbol> weight) {
    WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring<StackSymbol>> aut =
        new WeightedPAutomaton<StackSymbol, Abstraction, MinSemiring<StackSymbol>>() {

          @Override
          public Abstraction createState(Abstraction d, StackSymbol loc) {
            return new Abstraction(d, loc);
          }

          @Override
          public StackSymbol epsilon() {
            return s("EPS");
          }

		@Override
		public MinSemiring<StackSymbol> getZero() {
			return MinSemiring.zero();
		}

		@Override
		public MinSemiring<StackSymbol> getOne() {
			return MinSemiring.one();
		}

		@Override
		public boolean isGenereatedState(Abstraction d) {
			return d.s != null;
		}
        };
        aut.setInitialState(a(a));
        aut.addFinalState(ACC);
    aut.addTransition(t(a, c, ACC));
    aut.addWeightForTransition(t(a, c, ACC), weight);
    return aut;
  }

  static NormalRule<StackSymbol, Abstraction, MinSemiring<StackSymbol>> wnormal(int a, String n,
      int b, String m, MinSemiring<StackSymbol> w) {
    return new NormalRule<StackSymbol, Abstraction, MinSemiring<StackSymbol>>(a(a), s(n), a(b),
        s(m),
        w);
  }

  static PushRule<StackSymbol, Abstraction, MinSemiring<StackSymbol>> wpush(int a, String n, int b,
      String m, String l, MinSemiring<StackSymbol> w) {
    return new PushRule<StackSymbol, Abstraction, MinSemiring<StackSymbol>>(a(a), s(n), a(b), s(m),
        s(l), w);
  }

  static PopRule<StackSymbol, Abstraction, MinSemiring<StackSymbol>> wpop(int a, String n, int b,
      MinSemiring<StackSymbol> w) {
    return new PopRule<StackSymbol, Abstraction, MinSemiring<StackSymbol>>(a(a), s(n), a(b), w);
  }
}
