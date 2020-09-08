package boomerang.callgraph;

import boomerang.controlflowgraph.ObservableControlFlowGraph;
import boomerang.scene.CallGraph;
import boomerang.scene.CallGraph.Edge;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interprocedural control-flow graph, for which caller-callee edges can be observed using {@link
 * CalleeListener} and {@link CallerListener}. Used for demand-driven call graph generation.
 *
 * <p>Starts with an graph only containing intraprocedual edges and uses a precomputed call graph to
 * derive callers.
 *
 * @author Melanie Bruns on 04.05.2018
 */
public class ObservableDynamicICFG implements ObservableICFG<Statement, Method> {

  private static final Logger logger = LoggerFactory.getLogger(ObservableDynamicICFG.class);

  private static final int IMPRECISE_CALL_GRAPH_WARN_THRESHOLD = 30;

  private int numberOfEdgesTakenFromPrecomputedCallGraph = 0;

  private CallGraphOptions options = new CallGraphOptions();
  private CallGraph demandDrivenCallGraph = new CallGraph();

  private final Multimap<Statement, CalleeListener<Statement, Method>> calleeListeners =
      HashMultimap.create();
  private final Multimap<Method, CallerListener<Statement, Method>> callerListeners =
      HashMultimap.create();

  private final ObservableControlFlowGraph cfg;
  private final ICallerCalleeResolutionStrategy resolutionStrategy;

  public ObservableDynamicICFG(
      ObservableControlFlowGraph cfg, ICallerCalleeResolutionStrategy resolutionStrategy) {
    this.cfg = cfg;
    this.resolutionStrategy = resolutionStrategy;
  }

  @Override
  public void addCalleeListener(CalleeListener<Statement, Method> listener) {
    if (!calleeListeners.put(listener.getObservedCaller(), listener)) {
      return;
    }

    // Notify the new listener about edges we already know
    Statement stmt = listener.getObservedCaller();
    Collection<Edge> edges = demandDrivenCallGraph.edgesOutOf(stmt);

    if (edges.size() > IMPRECISE_CALL_GRAPH_WARN_THRESHOLD) {
      logger.debug(
          "Call graph has more than {} callees at {}",
          IMPRECISE_CALL_GRAPH_WARN_THRESHOLD,
          listener.getObservedCaller());
      for (Edge e : Lists.newArrayList(edges)) {
        logger.trace("\t callee {}", e.tgt());
      }
    }

    for (Edge e : Lists.newArrayList(edges)) {
      listener.onCalleeAdded(stmt, e.tgt());
    }

    InvokeExpr ie = stmt.getInvokeExpr();
    // Now check if we need to find new edges
    if ((ie.isInstanceInvokeExpr())) {
      // If it was invoked on an object we might find new instances
      if (ie.isSpecialInvokeExpr()) {
        addCallIfNotInGraph(stmt, resolutionStrategy.resolveSpecialInvoke(ie));
      } else {
        // Query for callees of the unit and add edges to the graph
        for (Method method : resolutionStrategy.resolveInstanceInvoke(stmt)) {
          addCallIfNotInGraph(stmt, method);
        }
      }
    } else {
      // Call was not invoked on an object. Must be static
      addCallIfNotInGraph(stmt, resolutionStrategy.resolveStaticInvoke(ie));
    }
  }

  @Override
  public void addCallerListener(CallerListener<Statement, Method> listener) {
    if (!callerListeners.put(listener.getObservedCallee(), listener)) {
      return;
    }

    Method method = listener.getObservedCallee();

    logger.debug("Queried for callers of {}.", method);

    // Notify the new listener about what we already now
    Collection<Edge> edges = demandDrivenCallGraph.edgesInto(method);
    if (edges.size() > IMPRECISE_CALL_GRAPH_WARN_THRESHOLD) {
      logger.debug(
          "Call graph has more than {} caller of {}",
          IMPRECISE_CALL_GRAPH_WARN_THRESHOLD,
          listener.getObservedCallee());
      for (Edge e : edges) {
        logger.trace("\t callsite {}", e.src());
      }
    }
    for (Edge e : Lists.newArrayList(edges)) {
      listener.onCallerAdded(e.src(), method);
    }
  }

  /**
   * Returns true if the call was added to the call graph, false if it was already present and the
   * call graph did not change
   */
  protected boolean addCallIfNotInGraph(Statement caller, Method callee) {
    Edge edge = new Edge(caller, callee);
    if (!demandDrivenCallGraph.addEdge(edge)) {
      return false;
    }
    logger.debug("Added call from unit '{}' to method '{}'", caller, callee);
    // Notify all interested listeners, so ..
    // .. CalleeListeners interested in callees of the caller or the CallGraphExtractor that is
    // interested in any
    for (CalleeListener<Statement, Method> listener :
        Lists.newArrayList(calleeListeners.get(caller))) {
      listener.onCalleeAdded(caller, callee);
    }
    // .. CallerListeners interested in callers of the callee or the CallGraphExtractor that is
    // interested in any
    for (CallerListener<Statement, Method> listener :
        Lists.newArrayList(callerListeners.get(callee))) {
      listener.onCallerAdded(caller, callee);
    }
    return true;
  }

  protected void notifyNoCalleeFound(Statement s) {
    for (CalleeListener<Statement, Method> l : Lists.newArrayList(calleeListeners.get(s))) {
      l.onNoCalleeFound();
    }
  }

  @Override
  public boolean isCallStmt(Statement unit) {
    return unit.containsInvokeExpr();
  }

  @Override
  public boolean isExitStmt(Statement unit) {
    return unit.getMethod().getControlFlowGraph().getEndPoints().contains(unit);
  }

  @Override
  public boolean isStartPoint(Statement unit) {
    return unit.getMethod().getControlFlowGraph().getStartPoints().contains(unit);
  }

  @Override
  public Collection<Statement> getStartPointsOf(Method m) {
    return m.getControlFlowGraph().getStartPoints();
  }

  @Override
  public Collection<Statement> getEndPointsOf(Method m) {
    return m.getControlFlowGraph().getEndPoints();
  }

  @Override
  public int getNumberOfEdgesTakenFromPrecomputedGraph() {
    return numberOfEdgesTakenFromPrecomputedCallGraph;
  }

  @Override
  public void resetCallGraph() {
    demandDrivenCallGraph = new CallGraph();
    numberOfEdgesTakenFromPrecomputedCallGraph = 0;
    calleeListeners.clear();
    callerListeners.clear();
  }

  @Override
  public void computeFallback() {
    resolutionStrategy.computeFallback(this);
  }

  @Override
  public void addEdges(Edge e) {
    demandDrivenCallGraph.addEdge(e);
  }
}
