package wpds.impl;

import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public class UPushRule<N extends Location, D extends State> extends PushRule<N, D, NoWeight<N>> {

  public UPushRule(D s1, N l1, D s2, N l2, N callSite) {
    super(s1, l1, s2, l2, callSite, NoWeight.NO_WEIGHT);
  }


  @Override
  public String toString() {
    return "<" + s1 + ";" + l1 + ">-><" + s2 + ";" + l2 + ">";
  }

}
