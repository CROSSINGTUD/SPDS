package wpds.semiring;

import wpds.impl.Weight;

public class One<N> extends Weight<N> {

  public One(N from, N to) {
    super(from, to);
  }

  @Override
  public Weight<N> extendWith(Weight<N> other) {
    return other;
  }

  @Override
  public Weight<N> combineWith(Weight<N> other) {
    return other.combineWith(this);
  }

}
