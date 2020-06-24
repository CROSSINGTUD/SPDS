package boomerang.callgraph;

import boomerang.scene.CallGraph.Edge;
import java.util.Collection;

/**
 * An interprocedural control-flow graph, for which caller-callee edges can be observed using {@link
 * CalleeListener} and {@link CallerListener}. Can be used for demand-driven call graph generation.
 *
 * @param <N> Nodes in the CFG, typically {@link Unit} or {@link Block}
 * @param <M> Method representation
 * @author Melanie Bruns on 04.05.2018
 */
public interface ObservableICFG<N, M> {
  /** Registers a listener that will be notified whenever a callee is added */
  void addCalleeListener(CalleeListener<N, M> listener);

  /** Registers a listener that will be notified whenever a caller is added. */
  void addCallerListener(CallerListener<N, M> listener);

  /** Returns <code>true</code> if the given statement is a call site. */
  boolean isCallStmt(N stmt);

  /**
   * Returns <code>true</code> if the given statement leads to a method return (exceptional or not).
   * For backward analyses may also be start statements.
   */
  boolean isExitStmt(N stmt);

  /**
   * Returns true is this is a method's start statement. For backward analyses those may also be
   * return or throws statements.
   */
  boolean isStartPoint(N stmt);

  int getNumberOfEdgesTakenFromPrecomputedGraph();

  /**
   * Resets the call graph. Only affects the call graph if it was built demand-driven, otherwise
   * graph will remain unchanged. Demand-driven call graph will keep intraprocedual information, but
   * reset start with an empty call graph again.
   */
  void resetCallGraph();

  Collection<N> getStartPointsOf(M callee);

  Collection<N> getEndPointsOf(M flowReaches);

  void computeFallback();

  void addEdges(Edge e);
}
