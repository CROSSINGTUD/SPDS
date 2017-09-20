package wpds.impl;

import wpds.interfaces.Location;

public abstract class Weight<N extends Location> {
  private N from;
  private N to;

  Weight<N> extendWithIn(Weight<N> other) {
    Weight<N> extendWith = extendWith(other);
    extendWith.setRange(from, other.to);
    return extendWith;
  };

  Weight<N> combineWithIn(Weight<N> other) {
    // assert other.from.equals(from) && other.to.equals(to);
    Weight<N> extendWith = combineWith(other);
    extendWith.setRange(from, to);
    return extendWith;
  };
  public abstract Weight<N> extendWith(Weight<N> other);

  public abstract Weight<N> combineWith(Weight<N> other);

  void setRange(N from, N to) {
    this.from = from;
    this.to = to;
  }

  public static NoWeight NO_WEIGHT_ONE = new Weight.NoWeight();
  public static NoWeight NO_WEIGHT_ZERO = new Weight.NoWeight();

  public static class NoWeight<N extends Location> extends Weight<N> {
    @Override
    public Weight<N> extendWith(Weight<N> other) {
      return other;
    }

    @Override
    public Weight<N> combineWith(Weight<N> other) {
      return other;
    }

    @Override
    public String toString() {
      return "";
    }


  }
}
