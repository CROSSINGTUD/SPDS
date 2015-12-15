package data;

import wpds.interfaces.Weight;

public class Zero extends PDSSet {
  private static Zero zero;

  private Zero() {}

  public static Zero v() {
    if (zero == null)
      zero = new Zero();
    return zero;
  }

  @Override
  public Weight extendWith(Weight other) {
    return this;
  }

  @Override
  public Weight combineWith(Weight other) {
    return other;
  }

  @Override
  public String toString() {
    return "<ZERO>";
  }
}
