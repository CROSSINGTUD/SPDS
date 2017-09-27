package wpds.impl;

public abstract class Weight {
  public abstract Weight extendWith(Weight other);

  public abstract Weight combineWith(Weight other);


  public static NoWeight NO_WEIGHT_ONE = new Weight.NoWeight();
  public static NoWeight NO_WEIGHT_ZERO = new Weight.NoWeight();

  public static class NoWeight extends Weight {
    @Override
    public Weight extendWith(Weight other) {
      return other;
    }

    @Override
    public Weight combineWith(Weight other) {
      return other;
    }

    @Override
    public String toString() {
      return "";
    }


  }
}
