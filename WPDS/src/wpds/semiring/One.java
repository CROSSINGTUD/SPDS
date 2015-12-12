package wpds.semiring;

import wpds.interfaces.Weight;

public class One implements Weight {

  @Override
  public Weight extendWith(Weight other) {
    return other;
  }

  @Override
  public Weight combineWith(Weight other) {
    return other.combineWith(this);
  }

}
