package pathexpression;

import java.util.Set;

public interface LabeledGraph<N, V> {
  Set<Edge<N, V>> getEdges();

  Set<N> getNodes();

  V epsilon();

}
