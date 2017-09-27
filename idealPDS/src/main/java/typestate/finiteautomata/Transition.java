package typestate.finiteautomata;

public class Transition<State> implements ITransition<State> {
  private final State from;
  private final State to;

  public Transition(State from, State to) {
    this.from = from;
    this.to = to;
  }

  public State from() {
    return from;
  }

  public State to() {
    return to;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((from == null) ? 0 : from.hashCode());
    result = prime * result + ((to == null) ? 0 : to.hashCode());
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
    if (from == null) {
      if (other.from != null)
        return false;
    } else if (!from.equals(other.from))
      return false;
    if (to == null) {
      if (other.to != null)
        return false;
    } else if (!to.equals(other.to))
      return false;
    return true;
  }

  public String toString() {
    return "" + from + " to " + to;
  }

}
