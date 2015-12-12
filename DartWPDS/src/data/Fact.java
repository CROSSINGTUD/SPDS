package data;

import soot.Local;
import wpds.interfaces.State;

public class Fact implements State {

  public final static Fact REACHABLE = new Fact(null) {
    public String toString() {
      return "REACHABLE";
    };

    @Override
    public boolean equals(Object obj) {
      return obj == Fact.REACHABLE;
    }

    public int hashCode() {
      return 1000000001;
    };
  };
  public final static Fact TARGET = new Fact(null) {
    public String toString() {
      return "TARGET";
    };

    @Override
    public boolean equals(Object obj) {
      return obj == Fact.TARGET;
    }

    public int hashCode() {
      return 1000000000;
    };
  };
  private Local local;

  public Fact(Local local) {
    this.local = local;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((local == null) ? 0 : local.hashCode());
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
    Fact other = (Fact) obj;
    if (local == null) {
      if (other.local != null)
        return false;
    } else if (!local.equals(other.local))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return (local == null ? "" : local.toString());
  }
}
