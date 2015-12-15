package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

public class PushRule<N extends Location, D extends State, W extends Weight> extends Rule<N, D, W> {

  private N callSite;

  public PushRule(N l1, D s1, N callSite, N l2, D s2, W w) {
    super(l1, s1, l2, s2, w);
    this.callSite = callSite;
  }

  public N getCallSite() {
    return callSite;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    PushRule other = (PushRule) obj;
    if (callSite == null) {
      if (other.callSite != null)
        return false;
    } else if (!callSite.equals(other.callSite))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "<" + s1 + ";" + l1 + ">-><" + s2 + ";" + l2 + "." + callSite + ">(" + w + ")";
  }

}
