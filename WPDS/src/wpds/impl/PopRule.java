package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

public class PopRule<N extends Location, D extends State, W extends Weight> extends Rule<N, D, W> {

  public PopRule(N l1, D s1, D s2, W w) {
    super(l1, s1, null, s2, w);
  }

  @Override
  public String toString() {
    return "POP<" + s1 + "@" + l1 + ">-><" + s2 + ">(" + w + ")";
  }
}
