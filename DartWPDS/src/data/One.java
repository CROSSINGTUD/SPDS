package data;

import wpds.impl.Weight;

public class One extends PDSSet {
  private static One one;

  private One() {}

  public static One v() {
    if (one == null)
      one = new One();
    return one;
  }

  @Override
  public Weight extendWith(Weight other) {
    return other;
  }

  @Override
  public Weight combineWith(Weight other) {
    return this;
  }

  @Override
  public String toString() {
    return "<ONE>";
  }

}
