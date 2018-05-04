package boomerang.callgraph;

import soot.Kind;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.toolkits.graph.DirectedGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ObservableICFGImpl implements ObservableICFG<Unit, SootMethod>{

    /**
     * Signifies when we work with a precomputed graph, like an instance of
     *  {@link BiDiInterproceduralCFG}, which already contains the full call graph.
     *  All queries will be handled using this precomputed graph.
     */
    private boolean isWorkingWithPrecomputedGraph;

    /**
     * Another wrapped call graph. If available, this is used to handle all queries.
     */
    private BiDiInterproceduralCFG<Unit, SootMethod> precomputedGraph;

    private CallGraph callGraph = new CallGraph();

    private ArrayList<CalleeListener<Unit, SootMethod>> calleeListeners = new ArrayList<>();
    private ArrayList<CallerListener<Unit, SootMethod>> callerListeners = new ArrayList<>();


    //TODO: When do we need this?
    public ObservableICFGImpl(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
        this.isWorkingWithPrecomputedGraph = true;
        this.precomputedGraph = icfg;
    }

    //TODO: Do we need an enable exceptions flag like in JimpleBasedICFG?
    public ObservableICFGImpl(){

    }

    public ObservableICFGImpl(ObservableICFG<Unit,SootMethod> icfg) {
    }

    @Override
    public SootMethod getMethodOf(Unit unit) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.getMethodOf(unit);
        }
        return null;
    }

    @Override
    public List<Unit> getPredsOf(Unit unit) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.getPredsOf(unit);
        }
        return null;
    }

    @Override
    public List<Unit> getSuccsOf(Unit unit) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.getSuccsOf(unit);
        }
        return null;
    }

    @Override
    public void addCalleeListener(CalleeListener listener) {
        calleeListeners.add(listener);
        //TODO: Notify the new one about what we already now?
    }

    @Override
    public void addCallee(SootMethod callee, Unit caller) {
        //Notify all listeners
        for (CalleeListener<Unit, SootMethod> listener : calleeListeners){
            listener.onCalleeAdded(caller, callee);
        }
        //TODO: Do we only use this for virtual call sites? Do we need to enforce that?
        Edge edge = new Edge(getMethodOf(caller), caller, callee, Kind.VIRTUAL);
        callGraph.addEdge(edge);
    }

    @Override
    public void addCallerListener(CallerListener listener) {
        callerListeners.add(listener);
        //TODO: Notify the new one about what we already now?

    }

    @Override
    public void addCaller(SootMethod callee, Unit caller) {
        //Notify all listeners
        for (CalleeListener<Unit, SootMethod> listener : calleeListeners){
            listener.onCalleeAdded(caller, callee);
        }
        //TODO: Do we only use this for virtual call sites? Do we need to enforce that?
        Edge edge = new Edge(getMethodOf(caller), caller, callee, Kind.VIRTUAL);
        callGraph.addEdge(edge);
    }

    @Override
    public Set<Unit> getCallsFromWithin(SootMethod sootMethod) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.getCallsFromWithin(sootMethod);
        }
        return null;
    }

    @Override
    public Collection<Unit> getStartPointsOf(SootMethod sootMethod) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.getStartPointsOf(sootMethod);
        }
        return null;
    }

    @Override
    public Collection<Unit> getReturnSitesOfCallAt(Unit unit) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.getReturnSitesOfCallAt(unit);
        }
        return null;
    }

    @Override
    public boolean isCallStmt(Unit stmt) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.isCallStmt(stmt);
        }
        return false;
    }

    @Override
    public boolean isExitStmt(Unit stmt) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.isExitStmt(stmt);
        }
        return false;
    }

    @Override
    public boolean isStartPoint(Unit stmt) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.isStartPoint(stmt);
        }
        return false;
    }

    @Override
    public Set<Unit> allNonCallStartNodes() {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.allNonCallStartNodes();
        }
        return null;
    }

    @Override
    public boolean isFallThroughSuccessor(Unit stmt, Unit succ) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.isFallThroughSuccessor(stmt, succ);
        }
        return false;
    }

    @Override
    public boolean isBranchTarget(Unit stmt, Unit succ) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.isBranchTarget(stmt, succ);
        }
        return false;
    }

    @Override
    public Collection<Unit> getEndPointsOf(SootMethod sootMethod) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.getEndPointsOf(sootMethod);
        }
        return null;
    }

    @Override
    public List<Unit> getPredsOfCallAt(Unit u) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.getPredsOfCallAt(u);
        }
        return null;
    }

    @Override
    public Set<Unit> allNonCallEndNodes() {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.allNonCallEndNodes();
        }
        return null;
    }

    @Override
    public DirectedGraph<Unit> getOrCreateUnitGraph(SootMethod body) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.getOrCreateUnitGraph(body);
        }
        return null;
    }

    @Override
    public List<Value> getParameterRefs(SootMethod sootMethod) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.getParameterRefs(sootMethod);
        }
        return null;
    }

    @Override
    public boolean isReturnSite(Unit unit) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.isReturnSite(unit);
        }
        return false;
    }

    @Override
    public boolean isReachable(Unit u) {
        if (isWorkingWithPrecomputedGraph){
            return precomputedGraph.isReachable(u);
        }
        return false;
    }
}
