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
    public void addCalleeListener(CalleeListener listener) {
        calleeListeners.add(listener);
        //TODO: Notify the new one about what we already now?
    }

    @Override
    public void addCallerListener(CallerListener listener) {
        callerListeners.add(listener);
        //TODO: Notify the new one about what we already now?

    }

    @Override
    public void addCall(Unit caller, SootMethod callee) {
        //Notify all listeners
        for (CalleeListener<Unit, SootMethod> listener : calleeListeners){
            listener.onCalleeAdded(caller, callee);
        }
        for (CallerListener<Unit, SootMethod> listener : callerListeners){
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
    public Collection<Unit> getReturnSitesOfCallAt(Unit unit) {
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
    public boolean isFallThroughSuccessor(Unit stmt, Unit succ) {
        return false;
    }

    @Override
    public boolean isBranchTarget(Unit stmt, Unit succ) {
        return false;
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
    public DirectedGraph<Unit> getOrCreateUnitGraph(SootMethod body) {
        return null;
    }

    @Override
    public List<Value> getParameterRefs(SootMethod sootMethod) {
        return null;
    }

    @Override
    public boolean isReturnSite(Unit unit) {
        return false;
    }

    @Override
    public boolean isReachable(Unit u) {
        return false;
    }
}
