package data;

import soot.Unit;
import wpds.interfaces.Location;

public class Stmt implements Location {
  private Unit delegate;

  public Stmt(Unit delegate) {
    this.delegate = delegate;
  }

  public Unit getDelegate() {
    return delegate;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
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
    Stmt other = (Stmt) obj;
    if (delegate == null) {
      if (other.delegate != null)
        return false;
    } else if (!delegate.equals(other.delegate))
      return false;
    return true;
  }

}
