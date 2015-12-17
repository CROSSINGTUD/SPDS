package wpds.impl;

import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public class UNormalRule<N extends Location, D extends State> extends
 NormalRule<N, D, NoWeight<N>> {

  public UNormalRule(D s1, N l1, D s2, N l2) {
    super(s1, l1, s2, l2, NoWeight.NO_WEIGHT);
  }

  @Override
  public String toString() {
    return "<" + s1 + ";" + l1 + ">-><" + s2 + ";" + l2 + ">";
  }

}
