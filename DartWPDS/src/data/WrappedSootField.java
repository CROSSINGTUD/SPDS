package data;

import soot.SootField;
import wpds.interfaces.Location;

public class WrappedSootField implements Location {
  private SootField delegate;

  public static WrappedSootField ANYFIELD = new WrappedSootField() {
    public String toString() {
      return "*";
    };
  };

  private WrappedSootField() {}

  public WrappedSootField(SootField delegate) {
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
    WrappedSootField other = (WrappedSootField) obj;
    if (delegate == null) {
      if (other.delegate != null)
        return false;
    } else if (!delegate.equals(other.delegate))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return delegate.getName();
  }
}
