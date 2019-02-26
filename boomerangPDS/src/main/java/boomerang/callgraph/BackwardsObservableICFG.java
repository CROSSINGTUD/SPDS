package boomerang.callgraph;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class BackwardsObservableICFG implements ObservableICFG<Unit, SootMethod> {
    protected final ObservableICFG<Unit, SootMethod> delegate;

    public BackwardsObservableICFG(ObservableICFG<Unit, SootMethod> fwOICFG) {
        this.delegate = fwOICFG;
    }

    @Override
    public List<Unit> getSuccsOf(Unit n) {
        return this.delegate.getPredsOf(n);
    }

    @Override
    public Collection<Unit> getAllPrecomputedCallers(SootMethod sootMethod) {
        return delegate.getAllPrecomputedCallers(sootMethod);
    }

    @Override
    public Collection<Unit> getStartPointsOf(SootMethod m) {
        return this.delegate.getEndPointsOf(m);
    }

    @Override
    public boolean isExitStmt(Unit stmt) {
        return this.delegate.isStartPoint(stmt);
    }

    @Override
    public boolean isStartPoint(Unit stmt) {
        return this.delegate.isExitStmt(stmt);
    }

    @Override
    public Set<Unit> allNonCallStartNodes() {
        return this.delegate.allNonCallEndNodes();
    }

    @Override
    public List<Unit> getPredsOf(Unit u) {
        return this.delegate.getSuccsOf(u);
    }

    @Override
    public Collection<Unit> getEndPointsOf(SootMethod m) {
        return this.delegate.getStartPointsOf(m);
    }

    @Override
    public Set<Unit> allNonCallEndNodes() {
        return this.delegate.allNonCallStartNodes();
    }

    @Override
    public SootMethod getMethodOf(Unit n) {
        return this.delegate.getMethodOf(n);
    }

    @Override
    public Set<Unit> getCallsFromWithin(SootMethod m) {
        return this.delegate.getCallsFromWithin(m);
    }

    @Override
    public boolean isCallStmt(Unit stmt) {
        return this.delegate.isCallStmt(stmt);
    }

    @Override
    public List<Value> getParameterRefs(SootMethod m) {
        return this.delegate.getParameterRefs(m);
    }

    @Override
    public boolean isReachable(Unit u) {
        return this.delegate.isReachable(u);
    }

    @Override
    public CallGraph getCallGraphCopy() {
        return delegate.getCallGraphCopy();
    }

    @Override
    public void addCalleeListener(CalleeListener listener) {
        delegate.addCalleeListener(listener);
    }

    @Override
    public void addCallerListener(CallerListener listener) {
        delegate.addCallerListener(listener);
    }

    @Override
	public boolean isMethodsWithCallFlow(SootMethod method) {
		return delegate.isMethodsWithCallFlow(method);
	}

	@Override
	public void addMethodWithCallFlow(SootMethod method) {
		delegate.addMethodWithCallFlow(method);
	}

    @Override
    public int getNumberOfEdgesTakenFromPrecomputedGraph() {
        return delegate.getNumberOfEdgesTakenFromPrecomputedGraph();
    }

    @Override
    public void resetCallGraph() {
        delegate.resetCallGraph();
    }

}
