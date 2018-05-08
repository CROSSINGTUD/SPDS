package boomerang.callgraph;

import boomerang.solver.BackwardBoomerangSolver;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import heros.DontSynchronize;
import heros.SynchronizedBy;
import heros.solver.IDESolver;
import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.exceptions.UnitThrowAnalysis;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObservableDynamicICFG implements ObservableICFG<Unit, SootMethod>{

    private CallGraph callGraph = new CallGraph();

    private BackwardBoomerangSolver solver;

    private ArrayList<CalleeListener<Unit, SootMethod>> calleeListeners = new ArrayList<>();
    private ArrayList<CallerListener<Unit, SootMethod>> callerListeners = new ArrayList<>();

    protected final boolean enableExceptions;

    @DontSynchronize("written by single thread; read afterwards")
    protected final Map<Unit,Body> unitToOwner = new HashMap<>();

    @SynchronizedBy("by use of synchronized LoadingCache class")
    protected final LoadingCache<Body,DirectedGraph<Unit>> bodyToUnitGraph = IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Body,DirectedGraph<Unit>>() {
        @Override
        public DirectedGraph<Unit> load(Body body) throws Exception {
            return makeGraph(body);
        }
    });

    @SynchronizedBy("by use of synchronized LoadingCache class")
    protected final LoadingCache<SootMethod,List<Value>> methodToParameterRefs = IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,List<Value>>() {
        @Override
        public List<Value> load(SootMethod m) throws Exception {
            return m.getActiveBody().getParameterRefs();
        }
    });

    @SynchronizedBy("by use of synchronized LoadingCache class")
    protected final LoadingCache<SootMethod,Set<Unit>> methodToCallsFromWithin = IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,Set<Unit>>() {
        @Override
        public Set<Unit> load(SootMethod m) throws Exception {
            Set<Unit> res = null;
            for(Unit u: m.getActiveBody().getUnits()) {
                if(isCallStmt(u)) {
                    if (res == null)
                        res = new LinkedHashSet<>();
                    res.add(u);
                }
            }
            return res == null ? Collections.emptySet() : res;
        }
    });

    public ObservableDynamicICFG(BackwardBoomerangSolver solver) {
        this(solver, true);
    }

    public ObservableDynamicICFG(BackwardBoomerangSolver solver, boolean enableExceptions) {
        this.solver = solver;
        this.enableExceptions = enableExceptions;
    }

    @Override
    public SootMethod getMethodOf(Unit unit) {
        assert unitToOwner.containsKey(unit) : "Statement " + unit + " not in unit-to-owner mapping";
        Body b = unitToOwner.get(unit);
        return b == null ? null : b.getMethod();
    }

    @Override
    public List<Unit> getPredsOf(Unit unit) {
        assert unit != null;
        Body body = unitToOwner.get(unit);
        DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(body);
        return unitGraph.getPredsOf(unit);
    }

    @Override
    public List<Unit> getSuccsOf(Unit unit) {
        Body body = unitToOwner.get(unit);
        if (body == null)
            return Collections.emptyList();
        DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(body);
        return unitGraph.getSuccsOf(unit);
    }

    private DirectedGraph<Unit> getOrCreateUnitGraph(Body body) {
        return bodyToUnitGraph.getUnchecked(body);
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
        return methodToCallsFromWithin.getUnchecked(sootMethod);
    }

    @Override
    public Collection<Unit> getStartPointsOf(SootMethod sootMethod) {
        if(sootMethod.hasActiveBody()) {
            Body body = sootMethod.getActiveBody();
            DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(body);
            return unitGraph.getHeads();
        }
        return Collections.emptySet();
    }

    @Override
    public boolean isCallStmt(Unit unit) {
        return ((Stmt)unit).containsInvokeExpr();
    }

    @Override
    public boolean isExitStmt(Unit unit) {
        Body body = unitToOwner.get(unit);
        DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(body);
        return unitGraph.getTails().contains(unit);
    }

    @Override
    public boolean isStartPoint(Unit unit) {
        Body body = unitToOwner.get(unit);
        DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(body);
        return unitGraph.getHeads().contains(unit);
    }

    @Override
    public Set<Unit> allNonCallStartNodes() {
        Set<Unit> res = new LinkedHashSet<>(unitToOwner.keySet());
        res.removeIf(u -> isStartPoint(u) || isCallStmt(u));
        return res;
    }

    @Override
    public Collection<Unit> getEndPointsOf(SootMethod sootMethod) {
        if(sootMethod.hasActiveBody()) {
            Body body = sootMethod.getActiveBody();
            DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(body);
            return unitGraph.getTails();
        }
        return Collections.emptySet();
    }

    @Override
    public Set<Unit> allNonCallEndNodes() {
        Set<Unit> res = new LinkedHashSet<>(unitToOwner.keySet());
        res.removeIf(u -> isExitStmt(u) || isCallStmt(u));
        return res;
    }

    @Override
    public List<Value> getParameterRefs(SootMethod sootMethod) {
        return methodToParameterRefs.getUnchecked(sootMethod);
    }

    @Override
    public boolean isReachable(Unit u) {
        return unitToOwner.containsKey(u);
    }

    protected DirectedGraph<Unit> makeGraph(Body body) {
        return enableExceptions
                ? new ExceptionalUnitGraph(body, UnitThrowAnalysis.v() ,true)
                : new BriefUnitGraph(body);
    }
}
