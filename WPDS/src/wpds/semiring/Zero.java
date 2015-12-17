package wpds.semiring;

import wpds.impl.Weight;

public class Zero<W extends Weight> implements Weight {
  private static Zero zero;

  private Zero() {}

  public static <W extends Weight> Zero<W> v() {
    if (zero == null)
      zero = new Zero<W>();
    return zero;
  }

  @Override
  public Weight extendWith(Weight other) {
    return zero;
  }

  @Override
  public Weight combineWith(Weight other) {
    return other;
  }

}
