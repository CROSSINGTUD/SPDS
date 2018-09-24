package boomerang.callgraph;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.Block;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * An interprocedural control-flow graph, for which caller-callee edges can be observed using {@link CalleeListener} and
 * {@link CallerListener}. Can be used for demand-driven call graph generation.
 *
 * @param <N> Nodes in the CFG, typically {@link Unit} or {@link Block}
 * @param <M> Method representation
 *
 * @author Melanie Bruns on 04.05.2018
 */
public interface ObservableICFG<N,M> {

    /**
     * Returns the method containing a node.
     * @param n The node for which to get the parent method
     */
    M getMethodOf(N n);

    List<N> getPredsOf(N u);

    /**
     * Returns the successor nodes.
     */
    List<N> getSuccsOf(N n);

    /**
     * Registers a listener that will be notified whenever a callee is added
     */
    void addCalleeListener(CalleeListener<N,M> listener);

    /**
     * Registers a listener that will be notified whenever a caller is added.
     */
    void addCallerListener(CallerListener<N,M> listener);

    /**
     * Returns all precomputed callers for a given method.
     * Behaviour from {@link #addCalleeListener(CalleeListener)} only differs when using dynamic call graph.
     */
    Collection<N> getAllPrecomputedCallers(M m);

    /**
     * Returns all call sites within a given method.
     */
    Set<N> getCallsFromWithin(M m);

    /**
     * Returns all start points of a given method. There may be
     * more than one start point in case of a backward analysis.
     */
    Collection<N> getStartPointsOf(M m);

    /**
     * Returns <code>true</code> if the given statement is a call site.
     */
    boolean isCallStmt(N stmt);

    /**
     * Returns <code>true</code> if the given statement leads to a method return
     * (exceptional or not). For backward analyses may also be start statements.
     */
    boolean isExitStmt(N stmt);

    /**
     * Returns true is this is a method's start statement. For backward analyses
     * those may also be return or throws statements.
     */
    boolean isStartPoint(N stmt);

    /**
     * Returns the set of all nodes that are neither call nor start nodes.
     */
    Set<N> allNonCallStartNodes();

    Collection<N> getEndPointsOf(M m);

    Set<N> allNonCallEndNodes();

    /**
     * Returns the list of parameter references used in the method's body. The list is as long as
     * the number of parameters declared in the associated method's signature.
     * The list may have <code>null</code> entries for parameters not referenced in the body.
     * The returned list is of fixed size.
     */
    List<Value> getParameterRefs(M m);

    /**
     * Checks whether the given statement is reachable from the entry point
     * @param u The statement to check
     * @return True if there is a control flow path from the entry point of the
     * program to the given statement, otherwise false
     */
    boolean isReachable(N u);

    CallGraph getCallGraphCopy();

	boolean isMethodsWithCallFlow(SootMethod method);

	void addMethodWithCallFlow(SootMethod method);

	int getNumberOfEdgesTakenFromPrecomputedGraph();

    /**
     * Resets the call graph. Only affects the call graph if it was built demand-driven, otherwise
     * graph will remain unchanged. Demand-driven call graph will keep intraprocedual information, but reset
     * start with an empty call graph again.
     */
	void resetCallGraph();

}
