package wpds.interfaces;

public interface Weight {
  public Weight extendWith(Weight other);

  public Weight combineWith(Weight other);
}
