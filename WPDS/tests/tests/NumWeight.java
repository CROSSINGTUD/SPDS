package tests;

import wpds.impl.Weight;
import wpds.interfaces.Location;

public class NumWeight<N extends Location> extends Weight<N> {

  int i;

  public NumWeight(int i) {
    this.i = i;
  }

  private NumWeight() {
  }
  @Override
  public Weight<N> extendWith(Weight<N> other) {
    if (this.equals(zero()) || other.equals(zero()))
      return zero();

    NumWeight<N> o = (NumWeight<N>) other;
    System.out.println("EXTEND " + this + "  " + other);
    return new NumWeight<N>(o.i + i);
  }

  @Override
  public Weight<N> combineWith(Weight<N> other) {
    System.out.println("COMBINE" + this + " " + other);
    if (other.equals(zero()))
      return this;
    if (this.equals(zero()))
      return other;
    NumWeight<N> o = (NumWeight<N>) other;
    if (o.i == i)
      return o;
    System.out.println(o.i);
    System.out.println(i);
    return zero();
  }

  private static NumWeight one;

  public static <N extends Location> NumWeight<N> one() {
    if (one == null)
      one = new NumWeight<N>() {
        @Override
        public String toString() {
          return "<ONE>";
        }
      };
    return one;
  }


  private static NumWeight zero;

  public static <N extends Location> NumWeight<N> zero() {
    if (zero == null)
      zero = new NumWeight<N>() {
      @Override
      public String toString() {
          return "<ZERO>";
      }

    };
    return zero;
  }

  @Override
  public String toString() {
    return Integer.toString(i);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + i;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    NumWeight other = (NumWeight) obj;
    if (i != other.i)
      return false;
    return true;
  }

}
