package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

public class NormalRule<N extends Location, D extends State, W extends Weight> extends
    Rule<N, D, W> {

  public NormalRule(N l1, D s1, N l2, D s2, W w) {
    super(l1, s1, l2, s2, w);
  }

  @Override
  public String toString() {
    return "<" + s1 + ";" + l1 + ">-><" + s2 + ";" + l2 + ">"
        + ((w instanceof Weight.NoWeight) ? "" : "(" + w + ")");
  }
}
