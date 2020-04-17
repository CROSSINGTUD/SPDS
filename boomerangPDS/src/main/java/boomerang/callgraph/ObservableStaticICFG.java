package boomerang.callgraph;

import boomerang.scene.CallGraph;
import boomerang.scene.CallGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interprocedural control-flow graph, for which caller-callee edges can be observed using {@link
 * CalleeListener} and {@link CallerListener}. This call graph wraps a precomputed call graph and
 * notifies listeners about all interprocedual edges for the requested relation at once.
 *
 * @author Melanie Bruns on 04.05.2018
 */
public class ObservableStaticICFG implements ObservableICFG<Statement, Method> {
  /** Wrapped static ICFG. If available, this is used to handle all queries. */
  private CallGraph precomputedGraph;

  private static final Logger LOGGER = LoggerFactory.getLogger(ObservableStaticICFG.class);
  private static final int IMPRECISE_CALL_GRAPH_WARN_THRESHOLD = 30000;

  public ObservableStaticICFG(CallGraph icfg) {
    this.precomputedGraph = icfg;
  }

  @Override
  public void addCalleeListener(CalleeListener<Statement, Method> listener) {
    Collection<Edge> edges = precomputedGraph.edgesOutOf(listener.getObservedCaller());
    if (edges.size() > IMPRECISE_CALL_GRAPH_WARN_THRESHOLD) {
      LOGGER.debug(
          "Call graph has more than {} callees at {}",
          IMPRECISE_CALL_GRAPH_WARN_THRESHOLD,
          listener.getObservedCaller());
      for (Edge e : edges) {
        LOGGER.trace("\t callee {}", e.tgt());
      }
    }
    for (boomerang.scene.CallGraph.Edge e : edges) {
      listener.onCalleeAdded(listener.getObservedCaller(), e.tgt());
    }
    if (edges.size() == 0) {
      listener.onNoCalleeFound();
    }
  }

  @Override
  public void addCallerListener(CallerListener<Statement, Method> listener) {
    Collection<Edge> edges = precomputedGraph.edgesInto(listener.getObservedCallee());
    if (edges.size() > IMPRECISE_CALL_GRAPH_WARN_THRESHOLD) {
      LOGGER.debug(
          "Call graph has more than {} caller of {}",
          IMPRECISE_CALL_GRAPH_WARN_THRESHOLD,
          listener.getObservedCallee());
      for (Edge e : edges) {
        LOGGER.trace("\t callsite {}", e.src());
      }
    }
    for (boomerang.scene.CallGraph.Edge e : edges) {
      listener.onCallerAdded(e.src(), listener.getObservedCallee());
    }
  }

  @Override
  public Collection<Statement> getStartPointsOf(Method m) {
    return m.getControlFlowGraph().getStartPoints();
  }

  @Override
  public boolean isCallStmt(Statement stmt) {
    return stmt.containsInvokeExpr();
  }

  @Override
  public boolean isExitStmt(Statement stmt) {
    return stmt.getMethod().getControlFlowGraph().getEndPoints().contains(stmt);
  }

  @Override
  public boolean isStartPoint(Statement stmt) {
    return stmt.getMethod().getControlFlowGraph().getStartPoints().contains(stmt);
  }

  @Override
  public Collection<Statement> getEndPointsOf(Method m) {
    return m.getControlFlowGraph().getEndPoints();
  }

  /**
   * Returns negative number to signify all edges are precomputed. CallGraphDebugger will add the
   * actual number in.
   *
   * @return -1 as all edges are precomputed, but we don't have access to the actual number
   */
  @Override
  public int getNumberOfEdgesTakenFromPrecomputedGraph() {
    return -1;
  }

  @Override
  public void resetCallGraph() {
    // Static call graph does not need to be reset, ignore this
  }

  @Override
  public void computeFallback() {
    // TODO Auto-generated method stub

  }

  @Override
  public void addEdges(Edge e) {
    throw new RuntimeException("Unnecessary");
  }
}
