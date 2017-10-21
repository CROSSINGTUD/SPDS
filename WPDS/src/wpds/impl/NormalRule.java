package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public class NormalRule<N extends Location, D extends State, W extends Weight> extends
    Rule<N, D, W> {

  public NormalRule(D s1, N l1, D s2, N l2, W w) {
    super(s1, l1, s2, l2, w);
  }

  @Override
  public String toString() {
    return "<" + s1 + ";" + l1 + ">-><" + s2 + ";" + l2 + ">"
        + ((w instanceof Weight.NoWeight) ? "" : "(" + w + ")");
  }

	public boolean canBeApplied(Transition<N, D> t, W weight) {
		return true;
	}
}
