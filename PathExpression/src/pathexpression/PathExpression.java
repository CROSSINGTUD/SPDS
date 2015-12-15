package heros.demandide.pathexpression;

public class PathExpression<N, V> {
  private RegEx<V> ex;
  private N w;
  private N u;

  public RegEx<V> getExpression() {
    return ex;
  }

  public N getTarget() {
    return w;
  }

  public N getSource() {
    return u;
  }


  public PathExpression(RegEx<V> reg, N u, N w) {
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
    result = prime * result + ((u == null) ? 0 : u.hashCode());
    result = prime * result + ((w == null) ? 0 : w.hashCode());
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
    if (u == null) {
      if (other.u != null)
        return false;
    } else if (!u.equals(other.u))
      return false;
    if (w == null) {
      if (other.w != null)
        return false;
    } else if (!w.equals(other.w))
      return false;
    return true;
  }



}
