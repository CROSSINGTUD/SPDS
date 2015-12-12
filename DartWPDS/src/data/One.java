package data;

import wpds.interfaces.Weight;

public class One extends Access {
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

}
