package data;

import soot.Printer;
import soot.Unit;
import wpds.interfaces.State;

public class AccessStmt implements State {
  private Unit delegate;
  private WrappedSootField field;

  public AccessStmt(Unit delegate) {
    this.delegate = delegate;
  }

  public AccessStmt(AccessStmt other, WrappedSootField field) {
    this.delegate = other.delegate;
    this.field = field;
  }

  public final static AccessStmt TARGET = new AccessStmt(null) {
    public String toString() {
      return "TARGET";
    };

    @Override
    public boolean equals(Object obj) {
      return obj == AccessStmt.TARGET;
    }

    public int hashCode() {
      return 1000000006;
    };
  };

  @Override
  public String toString() {
    if (field == null)
      return Printer.v().getOrCreatNumberFor(delegate).toString();
    return "<" + Printer.v().getOrCreatNumberFor(delegate).toString()
        + (field == null ? "" : "," + field.toString()) + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
    result = prime * result + ((field == null) ? 0 : field.hashCode());
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
    if (field == null) {
      if (other.field != null)
        return false;
    } else if (!field.equals(other.field))
      return false;
    return true;
  }

}
