package pathexpression;

public interface Edge<N, V> {
  public N getStart();

  public N getTarget();

  public V getLabel();
}
