package wpds.interfaces;

public interface Weight {
  public Weight extendWith(Weight other);

  public Weight combineWith(Weight other);

  public static NoWeight NO_WEIGHT = new Weight.NoWeight();
  public static NoWeight NO_WEIGHT_ZERO = new Weight.NoWeight();

  class NoWeight implements Weight {


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
