package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

public class Transition<N extends Location, D extends State, W extends Weight> {
  private D s1;
  private N l1;
  private D s2;

  public Transition(D s1, N l1, D s2) {
    this.s1 = s1;
    this.l1 = l1;
    this.s2 = s2;

  }

  public Configuration<N, D> getStartConfig() {
    return new Configuration<N, D>(l1, s1);
  }

  public D getTarget() {
    return s2;
  }

  public D getStart() {
    return s1;
  }

  public N getString() {
    return l1;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((l1 == null) ? 0 : l1.hashCode());
    result = prime * result + ((s1 == null) ? 0 : s1.hashCode());
    result = prime * result + ((s2 == null) ? 0 : s2.hashCode());
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
    Transition other = (Transition) obj;
    if (l1 == null) {
      if (other.l1 != null)
        return false;
    } else if (!l1.equals(other.l1))
      return false;
    if (s1 == null) {
      if (other.s1 != null)
        return false;
    } else if (!s1.equals(other.s1))
      return false;
    if (s2 == null) {
      if (other.s2 != null)
        return false;
    } else if (!s2.equals(other.s2))
      return false;
    return true;
  }

  @Override
  public String toString() {
    // TODO Auto-generated method stub
    return s1 + "~" + l1 + "~" + s2;
  }
}
