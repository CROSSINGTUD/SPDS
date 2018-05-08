package boomerang.callgraph;

import boomerang.solver.BackwardBoomerangSolver;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.DirectedGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ObservableDynamicICFG implements ObservableICFG<Unit, SootMethod>{

    private CallGraph callGraph = new CallGraph();

    private BackwardBoomerangSolver solver;

    private ArrayList<CalleeListener<Unit, SootMethod>> calleeListeners = new ArrayList<>();
    private ArrayList<CallerListener<Unit, SootMethod>> callerListeners = new ArrayList<>();

    public ObservableDynamicICFG(BackwardBoomerangSolver solver) {
        this.solver = solver;
    }

    @Override
    public SootMethod getMethodOf(Unit unit) {
        return null;
    }

    @Override
    public List<Unit> getPredsOf(Unit unit) {
        return null;
    }

    @Override
    public List<Unit> getSuccsOf(Unit unit) {
        return null;
    }

    @Override
    public void addCalleeListener(CalleeListener<Unit, SootMethod> listener) {
        calleeListeners.add(listener);
        //TODO: Notify the new one about what we already now?
    }

    @Override
    public void addCallerListener(CallerListener<Unit, SootMethod> listener) {
        callerListeners.add(listener);
        //TODO: Notify the new one about what we already now?

    }

    @Override
    public void addCall(Unit caller, SootMethod callee) {
        //Notify all interested listeners, so ..
        //.. CalleeListeners interested in callees of the caller or the CallGraphExtractor that is interested in any
        for (CalleeListener<Unit, SootMethod> listener : calleeListeners){
            if (CallGraphExtractor.ALL_UNITS.equals(caller) || caller.equals(listener.getObservedCaller()))
                listener.onCalleeAdded(caller, callee);
        }
        // .. CallerListeners interested in callers of the callee or the CallGraphExtractor that is interested in any
        for (CallerListener<Unit, SootMethod> listener : callerListeners){
            if (CallGraphExtractor.ALL_METHODS.equals(callee) || callee.equals(listener.getObservedCallee()))
                listener.onCallerAdded(caller, callee);
        }
        //TODO: Check this cast!
        Edge edge = new Edge(getMethodOf(caller), (Stmt)caller, callee);
        callGraph.addEdge(edge);
    }

    @Override
    public Set<Unit> getCallsFromWithin(SootMethod sootMethod) {
        return null;
    }

    @Override
    public Collection<Unit> getStartPointsOf(SootMethod sootMethod) {
        return null;
    }

    @Override
    public boolean isCallStmt(Unit stmt) {
        return false;
    }

    @Override
    public boolean isExitStmt(Unit stmt) {
        return false;
    }

    @Override
    public boolean isStartPoint(Unit stmt) {
        return false;
    }

    @Override
    public Set<Unit> allNonCallStartNodes() {
        return null;
    }

    @Override
    public Collection<Unit> getEndPointsOf(SootMethod sootMethod) {
        return null;
    }

    @Override
    public Set<Unit> allNonCallEndNodes() {
        return null;
    }

    @Override
    public List<Value> getParameterRefs(SootMethod sootMethod) {
        return null;
    }

    @Override
    public boolean isReachable(Unit u) {
        return false;
    }
}
