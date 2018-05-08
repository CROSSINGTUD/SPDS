package boomerang.callgraph;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.toolkits.graph.DirectedGraph;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BackwardsObservableICFG implements ObservableICFG<Unit, SootMethod> {
    protected final ObservableICFG<Unit, SootMethod> delegate;

    public BackwardsObservableICFG(ObservableICFG<Unit, SootMethod> fwOICFG) {
        this.delegate = fwOICFG;
    }

    public List<Unit> getSuccsOf(Unit n) {
        return this.delegate.getPredsOf(n);
    }

    public Collection<Unit> getStartPointsOf(SootMethod m) {
        return this.delegate.getEndPointsOf(m);
    }

    public List<Unit> getReturnSitesOfCallAt(Unit n) {
        return this.delegate.getPredsOf(n);
    }

    public boolean isExitStmt(Unit stmt) {
        return this.delegate.isStartPoint(stmt);
    }

    public boolean isStartPoint(Unit stmt) {
        return this.delegate.isExitStmt(stmt);
    }

    public Set<Unit> allNonCallStartNodes() {
        return this.delegate.allNonCallEndNodes();
    }

    public List<Unit> getPredsOf(Unit u) {
        return this.delegate.getSuccsOf(u);
    }

    public Collection<Unit> getEndPointsOf(SootMethod m) {
        return this.delegate.getStartPointsOf(m);
    }

    public Set<Unit> allNonCallEndNodes() {
        return this.delegate.allNonCallStartNodes();
    }

    public SootMethod getMethodOf(Unit n) {
        return this.delegate.getMethodOf(n);
    }

    public Set<Unit> getCallsFromWithin(SootMethod m) {
        return this.delegate.getCallsFromWithin(m);
    }

    public boolean isCallStmt(Unit stmt) {
        return this.delegate.isCallStmt(stmt);
    }

    public DirectedGraph<Unit> getOrCreateUnitGraph(SootMethod m) {
        return this.delegate.getOrCreateUnitGraph(m);
    }

    public List<Value> getParameterRefs(SootMethod m) {
        return this.delegate.getParameterRefs(m);
    }

    public boolean isFallThroughSuccessor(Unit stmt, Unit succ) {
        throw new UnsupportedOperationException("not implemented because semantics unclear");
    }

    public boolean isBranchTarget(Unit stmt, Unit succ) {
        throw new UnsupportedOperationException("not implemented because semantics unclear");
    }

    public boolean isReturnSite(Unit n) {
        Iterator var2 = this.getSuccsOf(n).iterator();

        Unit pred;
        do {
            if (!var2.hasNext()) {
                return false;
            }

            pred = (Unit)var2.next();
        } while(!this.isCallStmt(pred));

        return true;
    }

    public boolean isReachable(Unit u) {
        return this.delegate.isReachable(u);
    }

    public void addCalleeListener(CalleeListener listener) {
        //TODO figure out backward way
    }

    @Override
    public void addCall(Unit caller, SootMethod callee) {
        //TODO figure out backward way
    }

    @Override
    public void addCallerListener(CallerListener listener) {
        //TODO figure out backward way
    }


}
