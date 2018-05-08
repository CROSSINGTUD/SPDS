package boomerang.callgraph;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.toolkits.graph.DirectedGraph;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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
    public void addCall(Unit caller, SootMethod callee) {
        throw new UnsupportedOperationException("Static ICFG should not get new calls");
    }

    @Override
    public void addCallListener(CallListener listener) {
        //TODO notify listener about all relevant callers at once
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
    public Collection<Unit> getReturnSitesOfCallAt(Unit unit) {
        return precomputedGraph.getReturnSitesOfCallAt(unit);
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
    public boolean isFallThroughSuccessor(Unit stmt, Unit succ) {
        return precomputedGraph.isFallThroughSuccessor(stmt, succ);
    }

    @Override
    public boolean isBranchTarget(Unit stmt, Unit succ) {
        return precomputedGraph.isBranchTarget(stmt, succ);
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
    public DirectedGraph<Unit> getOrCreateUnitGraph(SootMethod body) {
        return precomputedGraph.getOrCreateUnitGraph(body);
    }

    @Override
    public List<Value> getParameterRefs(SootMethod sootMethod) {
        return precomputedGraph.getParameterRefs(sootMethod);
    }

    @Override
    public boolean isReturnSite(Unit unit) {
        return precomputedGraph.isReturnSite(unit);
    }

    @Override
    public boolean isReachable(Unit u) {
        return precomputedGraph.isReachable(u);
    }
}
