package pathexpression;



public class RegEx<V> {
  private static class Union<V> extends RegEx<V> {
    private RegEx<V> b;
    private RegEx<V> a;

    public Union(RegEx<V> a, RegEx<V> b) {
      assert a != null;
      assert b != null;
      this.a = a;
      this.b = b;
    }

    public String toString() {
      return a.toString() + " U " + b.toString();
    }

    public RegEx<V> getFirst() {
      return a;
    }

    public RegEx<V> getSecond() {
      return b;
    }
  }
  private static class Concatenate<V> extends RegEx<V> {
    public RegEx<V> b;
    public RegEx<V> a;

    public Concatenate(RegEx<V> a, RegEx<V> b) {
      assert a != null;
      assert b != null;
      this.a = a;
      this.b = b;
    }

    public String toString() {
      return a.toString() + " . " + b.toString();
    }

    public RegEx<V> getFirst() {
      return a;
    }

    public RegEx<V> getSecond() {
      return b;
    }
  }
  private static class Star<V> extends RegEx<V> {
    public RegEx<V> a;

    public Star(RegEx<V> a) {
      assert a != null;
      this.a = a;
    }

    public String toString() {
      return "[" + a.toString() + "]* ";
    }

    public RegEx<V> getPlain() {
      return a;
    }
  }

  public static <V> RegEx<V> union(RegEx<V> a, RegEx<V> b) {
    assert a != null;
    assert b != null;
    if (a instanceof EmptySet)
      return b;
    if (b instanceof EmptySet)
      return a;
    return simplify(new Union<V>(a, b));
  }

  public static <V> RegEx<V> concatenate(RegEx<V> a, RegEx<V> b) {
    assert a != null;
    assert b != null;

    if (a instanceof Epsilon || a instanceof EmptySet)
      return b;

    if (b instanceof Epsilon || b instanceof EmptySet)
      return a;
    return simplify(new Concatenate<V>(a, b));
  }

  public static <V> RegEx<V> star(RegEx<V> reg) {
    if (reg instanceof EmptySet || reg instanceof Epsilon)
      return new Epsilon<V>();
    return simplify(new Star<V>(reg));
  }

  public static <V> RegEx<V> simplify(RegEx<V> in) {
    if (in instanceof Union) {
      Union<V> u = ((Union<V>) in);
      if (u.getFirst() instanceof EmptySet)
        return u.getSecond();
      if (u.getSecond() instanceof EmptySet)
        return u.getFirst();
    }
    if (in instanceof Concatenate) {
      Concatenate<V> c = (Concatenate<V>) in;
      RegEx<V> first = c.getFirst();
      if (first instanceof Epsilon || first instanceof EmptySet)
        return c.getSecond();
      RegEx<V> second = c.getSecond();
      if (second instanceof Epsilon || second instanceof EmptySet)
        return c.getFirst();
    }

    if (in instanceof Star) {
      Star<V> star = (Star<V>) in;
      if (star.getPlain() instanceof EmptySet || star.getPlain() instanceof Epsilon)
        return new Epsilon<V>();
    }

    return in;
  }

  public static class Plain<V> extends RegEx<V> {
    public V v;

    public Plain(V v) {
      assert v != null;
      this.v = v;
    }

    public String toString() {
      return v.toString();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((v == null) ? 0 : v.hashCode());
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
      Plain other = (Plain) obj;
      if (v == null) {
        if (other.v != null)
          return false;
      } else if (!v.equals(other.v))
        return false;
      return true;
    }
  }

  private static class Epsilon<V> extends RegEx<V> {
    public String toString() {
      return "EPSILON";
    }

  }
  private static class EmptySet<V> extends RegEx<V> {
    public String toString() {
      return "EMPTY";
    }
  }

  private static RegEx eps;

  public static <V> RegEx<V> epsilon() {
    if (eps == null)
      eps = new Epsilon<V>();
    return eps;
  }

  private static RegEx empty;

  public static <V> RegEx<V> emptySet() {
    if (empty == null)
      empty = new EmptySet<V>();
    return empty;
  }
}
