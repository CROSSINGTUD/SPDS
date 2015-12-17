package wpds.impl;

import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public class UPopRule<N extends Location, D extends State> extends PopRule<N, D, NoWeight<N>> {

  public UPopRule(D s1, N l1, D s2) {
    super(s1, l1, s2, NoWeight.NO_WEIGHT);
  }


  @Override
  public String toString() {
    return "<" + s1 + ";" + l1 + ">-><" + s2 + ">";
  }

}
