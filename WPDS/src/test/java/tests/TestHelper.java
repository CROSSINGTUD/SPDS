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

import wpds.impl.NormalRule;
import wpds.impl.PAutomaton;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.impl.Transition;
import wpds.impl.UNormalRule;
import wpds.impl.UPopRule;
import wpds.impl.UPushRule;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public class TestHelper {
  static Abstraction ACC = a(999);

  static PAutomaton<StackSymbol, Abstraction> accepts(int a, String c) {
    PAutomaton<StackSymbol, Abstraction> aut =
        new PAutomaton<StackSymbol, Abstraction>() {

          @Override
          public Abstraction createState(Abstraction d, StackSymbol loc) {
            return new Abstraction(d, loc);
          }

          @Override
          public StackSymbol epsilon() {
            return s("EPS");
          }

          @Override
          public boolean isGeneratedState(Abstraction d) {
            return d.s != null;
          }
        };
    aut.addFinalState(ACC);
    aut.addTransition(t(a, c, ACC));
    return aut;
  }

  static WeightedPAutomaton<StackSymbol, Abstraction, NumWeight> waccepts(
      int a, String c, NumWeight weight) {
    WeightedPAutomaton<StackSymbol, Abstraction, NumWeight> aut =
        new WeightedPAutomaton<StackSymbol, Abstraction, NumWeight>() {

          @Override
          public Abstraction createState(Abstraction d, StackSymbol loc) {
            return new Abstraction(d, loc);
          }

          @Override
          public StackSymbol epsilon() {
            return s("EPS");
          }

          @Override
          public NumWeight getOne() {
            return NumWeight.one();
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

  static Abstraction a(int a) {
    return new Abstraction(a);
  }

  static Abstraction a(int a, String b) {
    return new Abstraction(a(a), s(b));
  }

  static StackSymbol s(String a) {
    return new StackSymbol(a);
  }

  static Transition<StackSymbol, Abstraction> t(Abstraction a, StackSymbol c, Abstraction b) {
    return new Transition<StackSymbol, Abstraction>(a, c, b);
  }

  static Transition<StackSymbol, Abstraction> t(Abstraction a, String c, Abstraction b) {
    return new Transition<StackSymbol, Abstraction>(a, s(c), b);
  }

  static Transition<StackSymbol, Abstraction> t(int a, StackSymbol c, Abstraction b) {
    return t(a(a), c, b);
  }

  static Transition<StackSymbol, Abstraction> t(int a, String c, Abstraction b) {
    return t(a, s(c), b);
  }

  static Transition<StackSymbol, Abstraction> t(int a, String c, int b) {
    return t(a, c, a(b));
  }

  static UNormalRule<StackSymbol, Abstraction> normal(int a, String n, int b, String m) {
    return new UNormalRule<StackSymbol, Abstraction>(a(a), s(n), a(b), s(m));
  }

  static UPushRule<StackSymbol, Abstraction> push(int a, String n, int b, String m, String l) {
    return new UPushRule<StackSymbol, Abstraction>(a(a), s(n), a(b), s(m), s(l));
  }

  static UPopRule<StackSymbol, Abstraction> pop(int a, String n, int b) {
    return new UPopRule<StackSymbol, Abstraction>(a(a), s(n), a(b));
  }

  static NormalRule<StackSymbol, Abstraction, NumWeight> wnormal(
      int a, String n, int b, String m, NumWeight w) {
    return new NormalRule<StackSymbol, Abstraction, NumWeight>(a(a), s(n), a(b), s(m), w);
  }

  static PushRule<StackSymbol, Abstraction, NumWeight> wpush(
      int a, String n, int b, String m, String l, NumWeight w) {
    return new PushRule<StackSymbol, Abstraction, NumWeight>(a(a), s(n), a(b), s(m), s(l), w);
  }

  static PopRule<StackSymbol, Abstraction, NumWeight> wpop(int a, String n, int b, NumWeight w) {
    return new PopRule<StackSymbol, Abstraction, NumWeight>(a(a), s(n), a(b), w);
  }

  static class Abstraction implements State {
    final int a;
    final StackSymbol s;

    Abstraction(int a) {
      this.a = a;
      this.s = null;
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
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Abstraction other = (Abstraction) obj;
      if (a != other.a) return false;
      if (s == null) {
        if (other.s != null) return false;
      } else if (!s.equals(other.s)) return false;
      return true;
    }
  }

  static class StackSymbol implements Location {
    String s;

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
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      StackSymbol other = (StackSymbol) obj;
      if (s == null) {
        if (other.s != null) return false;
      } else if (!s.equals(other.s)) return false;
      return true;
    }

    @Override
    public String toString() {
      return s;
    }

    @Override
    public boolean accepts(Location other) {
      return this.equals(other);
    }
  }
}
