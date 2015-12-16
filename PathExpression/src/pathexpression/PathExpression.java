package pathexpression;

class PathExpression<V> {
  private IRegEx<V> ex;
  private int w;
  private int u;

  public IRegEx<V> getExpression() {
    return ex;
  }

  public int getTarget() {
    return w;
  }

  public int getSource() {
    return u;
  }


  public PathExpression(IRegEx<V> reg, int u, int w) {
    this.ex = reg;
    this.u = u;
    this.w = w;
  }

  public String toString() {
    return "{" + u + "," + ex.toString() + "," + w + "}";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((ex == null) ? 0 : ex.hashCode());
    result = prime * result + u;
    result = prime * result + w;
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
    PathExpression other = (PathExpression) obj;
    if (ex == null) {
      if (other.ex != null)
        return false;
    } else if (!ex.equals(other.ex))
      return false;
    if (u != other.u)
      return false;
    if (w != other.w)
      return false;
    return true;
  }



}
