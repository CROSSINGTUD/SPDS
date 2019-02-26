package boomerang.callgraph;

import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An interprocedural control-flow graph, for which caller-callee edges can be observed using {@link CalleeListener} and
 * {@link CallerListener}. This call graph wraps a precomputed call graph and notifies listeners about all
 * interprocedual edges for the requested relation at once.
 *
 *
 * @author Melanie Bruns on 04.05.2018
 */
public class ObservableStaticICFG implements ObservableICFG<Unit, SootMethod>{
    /**
     * Wrapped static ICFG. If available, this is used to handle all queries.
     */
    private BiDiInterproceduralCFG<Unit, SootMethod> precomputedGraph;

    public ObservableStaticICFG(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
        this.precomputedGraph = icfg;
    }

    @Override
    public SootMethod getMethodOf(Unit unit) {
        return precomputedGraph.getMethodOf(unit);
    }

    @Override
    public List<Unit> getPredsOf(Unit unit) {
        return precomputedGraph.getPredsOf(unit);
    }

    @Override
    public List<Unit> getSuccsOf(Unit unit) {
        return precomputedGraph.getSuccsOf(unit);
    }

    @Override
    public void addCalleeListener(CalleeListener<Unit, SootMethod> listener) {
        for (SootMethod method : precomputedGraph.getCalleesOfCallAt(listener.getObservedCaller())){
            listener.onCalleeAdded(listener.getObservedCaller(), method);
        }
    }

    @Override
    public void addCallerListener(CallerListener<Unit, SootMethod> listener) {
        for (Unit unit : precomputedGraph.getCallersOf(listener.getObservedCallee())){
            listener.onCallerAdded(unit, listener.getObservedCallee());
        }
    }

    @Override
    public Collection<Unit> getAllPrecomputedCallers(SootMethod sootMethod) {
        return precomputedGraph.getCallersOf(sootMethod);
    }

    @Override
    public Set<Unit> getCallsFromWithin(SootMethod sootMethod) {
        return precomputedGraph.getCallsFromWithin(sootMethod);
    }

    @Override
    public Collection<Unit> getStartPointsOf(SootMethod sootMethod) {
        return precomputedGraph.getStartPointsOf(sootMethod);
    }

    @Override
    public boolean isCallStmt(Unit stmt) {
        return precomputedGraph.isCallStmt(stmt);
    }

    @Override
    public boolean isExitStmt(Unit stmt) {
        return precomputedGraph.isExitStmt(stmt);
    }

    @Override
    public boolean isStartPoint(Unit stmt) {
        return precomputedGraph.isStartPoint(stmt);
    }

    @Override
    public Set<Unit> allNonCallStartNodes() {
        return precomputedGraph.allNonCallStartNodes();
    }

    @Override
    public Collection<Unit> getEndPointsOf(SootMethod sootMethod) {
        return precomputedGraph.getEndPointsOf(sootMethod);
    }

    @Override
    public Set<Unit> allNonCallEndNodes() {
        return precomputedGraph.allNonCallEndNodes();
    }

    @Override
    public List<Value> getParameterRefs(SootMethod sootMethod) {
        return precomputedGraph.getParameterRefs(sootMethod);
    }

    @Override
    public boolean isReachable(Unit u) {
        return precomputedGraph.isReachable(u);
    }

    public CallGraph getCallGraphCopy(){
        CallGraph copy = new CallGraph();
        HashSet<SootMethod> visited = new HashSet<>();
        for (SootMethod entryPoint : Scene.v().getEntryPoints()){
            if (!visited.contains(entryPoint)){
                addEdgesForCallees(entryPoint, visited, copy);
            }
        }
        return copy;
    }

    private void addEdgesForCallees(SootMethod sootMethod, HashSet<SootMethod> visited, CallGraph copy) {
        visited.add(sootMethod);
        for (Unit callsite : precomputedGraph.getCallsFromWithin(sootMethod)){
            for (SootMethod callee : precomputedGraph.getCalleesOfCallAt(callsite)){
                copy.addEdge(new Edge(sootMethod, (Stmt)callsite, callee));
                if (!visited.contains(callee)){
                    addEdgesForCallees(callee, visited, copy);
                }
            }
        }
    }

	@Override
	public boolean isMethodsWithCallFlow(SootMethod method) {
		return false;
	}

	@Override
	public void addMethodWithCallFlow(SootMethod method) {
        //No need to keep track of that since we rely on the precomputed graph
	}

    /**
     * Returns negative number to signify all edges are precomputed. CallGraphDebugger will add the actual number in.
     * @return -1 as all edges are precomputed, but we don't have access to the actual number
     */
    @Override
    public int getNumberOfEdgesTakenFromPrecomputedGraph() {
        return -1;
    }

    @Override
    public void resetCallGraph() {
        //Static call graph does not need to be reset, ignore this
    }
}
