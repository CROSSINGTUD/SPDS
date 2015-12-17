package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import wpds.impl.PAutomaton;
import wpds.impl.PushdownSystem;
import wpds.impl.Transition;
import wpds.impl.UNormalRule;
import wpds.impl.UPopRule;
import wpds.impl.UPushRule;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public class PDSTests {

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
    PAutomaton<StackSymbol, Abstraction> fa = accepts(1, "a");
    pds.poststar(fa);
    System.out.println(fa);
    assertTrue(fa.getTransitions().contains(t(1, "d", ACC)));
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
    PAutomaton<StackSymbol, Abstraction> fa = accepts(1, "a");
    pds.poststar(fa);
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
    PAutomaton<StackSymbol, Abstraction> fa = accepts(1, "a");
    pds.poststar(fa);
    System.out.println(fa);
    assertTrue(fa.getTransitions().contains(t(1, "k", ACC)));
    assertTrue(fa.getTransitions().contains(t(1, "k", ACC)));
    assertTrue(fa.getTransitions().contains(t(1, fa.epsilon(), new Abstraction(a(1), s("d")))));
  }

  @Test
  public void recPushTestSimple() {
    pds.addRule(push(1, "a", 1, "d", "e"));
    pds.addRule(push(1, "d", 1, "d", "h"));
    pds.addRule(pop(1, "d", 1));
    pds.addRule(normal(1, "e", 1, "k"));
    PAutomaton<StackSymbol, Abstraction> fa = accepts(1, "a");
    pds.poststar(fa);
    System.out.println(fa);
  }
  private static Abstraction ACC = a(999);

  private static PAutomaton<StackSymbol, Abstraction> accepts(int a, String c) {
    return new PAutomaton<StackSymbol, Abstraction>(a(a), Collections.singleton(t(a, c, ACC)),
        ACC) {

      @Override
      public Abstraction createState(Abstraction d, StackSymbol loc) {
        return new Abstraction(d, loc);
      }

      @Override
      public StackSymbol epsilon() {
        return s("EPS");
      }
    };
  }

  private static Abstraction a(int a) {
    return new Abstraction(a);
  }

  private static StackSymbol s(String a) {
    return new StackSymbol(a);
  }

  private static Transition<StackSymbol, Abstraction> t(int a, StackSymbol c, Abstraction b) {
    return new Transition<StackSymbol, Abstraction>(a(a), c, b);
  }

  private static Transition<StackSymbol, Abstraction> t(int a, String c, Abstraction b) {
    return t(a, s(c), b);
  }

  private static Transition<StackSymbol, Abstraction> t(int a, String c, int b) {
    return t(a, c, a(b));
  }

  private static UNormalRule<StackSymbol, Abstraction> normal(int a, String n, int b, String m) {
    return new UNormalRule<StackSymbol, Abstraction>(a(a), s(n), a(b), s(m));
  }

  private static UPushRule<StackSymbol, Abstraction> push(int a, String n, int b, String m,
      String l) {
    return new UPushRule<StackSymbol, Abstraction>(a(a), s(n), a(b), s(m), s(l));
  }

  private static UPopRule<StackSymbol, Abstraction> pop(int a, String n, int b) {
    return new UPopRule<StackSymbol, Abstraction>(a(a), s(n), a(b));
  }

  private static class Abstraction implements State {
    private int a;
    private StackSymbol s;

    Abstraction(int a) {
      this.a = a;
    }

    Abstraction(Abstraction a, StackSymbol s) {
      this.s = s;
      this.a = a.a;
    }

    @Override
    public String toString() {
      return (s == null ? Integer.toString(a) : "<" + a + "," + s + ">");
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + a;
      result = prime * result + ((s == null) ? 0 : s.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Abstraction other = (Abstraction) obj;
      if (a != other.a)
        return false;
      if (s == null) {
        if (other.s != null)
          return false;
      } else if (!s.equals(other.s))
        return false;
      return true;
    }

  }
  private static class StackSymbol implements Location {
    private String s;

    StackSymbol(String s) {
      this.s = s;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((s == null) ? 0 : s.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      StackSymbol other = (StackSymbol) obj;
      if (s == null) {
        if (other.s != null)
          return false;
      } else if (!s.equals(other.s))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return s;
    }
  }
}
