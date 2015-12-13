package data;

import soot.Unit;
import wpds.interfaces.State;

public class AccessStmt implements State {
  private Unit delegate;

  public AccessStmt(Unit delegate) {
    this.delegate = delegate;
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
    AccessStmt other = (AccessStmt) obj;
    if (delegate == null) {
      if (other.delegate != null)
        return false;
    } else if (!delegate.equals(other.delegate))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "AccessStmt [delegate=" + delegate + "]";
  }

}
