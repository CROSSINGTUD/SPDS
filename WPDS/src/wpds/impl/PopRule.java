package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public class PopRule<N extends Location, D extends State, W extends Weight> extends Rule<N, D, W> {

  public PopRule(D s1, N l1, D s2, W w) {
    super(s1, l1, s2, null, w);
  }

  @Override
  public String toString() {
    return "<" + s1 + ";" + l1 + ">-><" + s2 + ">(" + w + ")";
  }
}
