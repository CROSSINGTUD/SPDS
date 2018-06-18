package boomerang.callgraph;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.WeightedBoomerang;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.BackwardBoomerangResults;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import heros.DontSynchronize;
import heros.SynchronizedBy;
import heros.solver.IDESolver;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.exceptions.UnitThrowAnalysis;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import wpds.impl.PAutomaton;

import java.util.*;

/**
 * An interprocedural control-flow graph, for which caller-callee edges can be observed using {@link CalleeListener} and
 * {@link CallerListener}. Used for demand-driven call graph generation.
 *
 *
 * Starts with an graph only containing intraprocedual edges and uses a CHA-based call graph to derive callers.
 *
 * @author Melanie Bruns on 04.05.2018
 */
public class ObservableDynamicICFG implements ObservableICFG<Unit, SootMethod>{

    private CallGraph demandDrivenCallGraph = new CallGraph();

    private CallGraph chaCallGraph;

    private WeightedBoomerang<?> solver;

    private ArrayList<CalleeListener<Unit, SootMethod>> calleeListeners = new ArrayList<>();
    private ArrayList<CallerListener<Unit, SootMethod>> callerListeners = new ArrayList<>();

    protected final boolean enableExceptions;

    @DontSynchronize("written by single thread; read afterwards")
    protected final Map<Unit,Body> unitToOwner = new HashMap<>();

    @SynchronizedBy("by use of synchronized LoadingCache class")
    protected final LoadingCache<Body,DirectedGraph<Unit>> bodyToUnitGraph = IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Body,DirectedGraph<Unit>>() {
        @Override
        public DirectedGraph<Unit> load(Body body){
            return makeGraph(body);
        }
    });

    @SynchronizedBy("by use of synchronized LoadingCache class")
    protected final LoadingCache<SootMethod,List<Value>> methodToParameterRefs = IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,List<Value>>() {
        @Override
        public List<Value> load(SootMethod m){
            return m.getActiveBody().getParameterRefs();
        }
    });

    @SynchronizedBy("by use of synchronized LoadingCache class")
    protected final LoadingCache<SootMethod,Set<Unit>> methodToCallsFromWithin = IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,Set<Unit>>() {
        @Override
        public Set<Unit> load(SootMethod m){
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

    public ObservableDynamicICFG(WeightedBoomerang solver) {
        this(solver, true);
    }

    public ObservableDynamicICFG(WeightedBoomerang solver, boolean enableExceptions) {
        this.solver = solver;
        this.enableExceptions = enableExceptions;

        this.chaCallGraph = Scene.v().getCallGraph();

        initializeUnitToOwner();
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

        //Notify the new listener about edges we already know
        Unit unit = listener.getObservedCaller();
        Iterator<Edge> edgeIterator = demandDrivenCallGraph.edgesOutOf(unit);
        while (edgeIterator.hasNext()){
            Edge edge = edgeIterator.next();
            listener.onCalleeAdded(unit, edge.tgt());
        }
        //If not all edges from the CHA call graph are covered, there may be more to discover
        if (!allEdgesCovered(chaCallGraph.edgesOutOf(unit), demandDrivenCallGraph.edgesOutOf(unit))){
            Stmt stmt = (Stmt) unit;
            //Making a query to see which type of object the method was invoked on only makes sense when it was
            //invoked on an object
            //TODO do we need to make sure not to use the dummy obj here?
            if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr){
                //Call was invoked on an object
                queryForCallees(unit);
            } else {
                //Call was not invoked on an object. Must be static or special. In this case rely on CHA //TODO for now?
                Iterator<Edge> chaIterator = chaCallGraph.edgesOutOf(unit);
                while (chaIterator.hasNext()){
                    Edge edge = chaIterator.next();
                    addCall(unit, edge.tgt());
                }
            }
        }
    }

    private void queryForCallees(Unit unit) {
        //Construct BackwardQuery, so we know which types the object might have
        Stmt stmt = (Stmt) unit;
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        Value value = ((InstanceInvokeExpr) invokeExpr).getBase();
        Val val = new Val(value, getMethodOf(stmt));
        Statement statement = new Statement(stmt, getMethodOf(unit));
        BackwardQuery query = new BackwardQuery(statement, val);

        //Execute that query
        BackwardBoomerangResults results = solver.solve(query);

        //Go through possible types an add edges to implementations in possible types
        Map<ForwardQuery, PAutomaton> allocationSites = results.getAllocationSites();
        for (ForwardQuery forwardQuery : allocationSites.keySet()){
            System.out.println("Forward Query found: "+forwardQuery);
            Type type = forwardQuery.getType();
            //TODO find a much cleaner way to do this. How to get method in correct type?
            Iterator<Edge> edgeIterator1 = chaCallGraph.edgesOutOf(unit);
            while (edgeIterator1.hasNext()){
                Edge edge = edgeIterator1.next();
                if (edge.tgt().getDeclaringClass().getType() == type){
                    addCall(unit, edge.tgt());
                    break;
                }
            }
            //TODO get callee in type
        }
    }

    @Override
    public void addCallerListener(CallerListener<Unit, SootMethod> listener) {
        callerListeners.add(listener);

        //Notify the new one about what we already now
        SootMethod method = listener.getObservedCallee();
        Iterator<Edge> edgeIterator = demandDrivenCallGraph.edgesInto(method);
        while (edgeIterator.hasNext()){
            Edge edge = edgeIterator.next();
            listener.onCallerAdded(edge.srcUnit(), method);
        }

        //TODO figure out when to query: When would we have enough context? How do we know?
        // Call BackwardQuery for all potential callers?

        //If not all edges from the CHA call graph are covered, there may be more to discover
        if (!allEdgesCovered(chaCallGraph.edgesInto(method), demandDrivenCallGraph.edgesInto(method))){
            //Therefore we use the solver
            Iterator<Edge> chaIterator = chaCallGraph.edgesInto(method);
            while (chaIterator.hasNext()){
                Edge edge = chaIterator.next();
                addCall(edge.srcUnit(), edge.tgt());
            }
            //TODO use solver to get potentially missing edges
        }
    }

    private boolean allEdgesCovered(Iterator<Edge> chaEdgeIterator, Iterator<Edge> knownEdgeIterator){
        //Make a map checking for every edge in the CHA call graph whether it is in the known edges

        //Start by assuming no edge is covered
        HashMap<Edge, Boolean> wasEdgeCovered = new HashMap<>();
        while (chaEdgeIterator.hasNext()){
            wasEdgeCovered.put(chaEdgeIterator.next(), false);
        }

        //Put true for all known edges
        while (knownEdgeIterator.hasNext()){
            wasEdgeCovered.put(knownEdgeIterator.next(), true);
        }

        //If any single edge is not covered, return false
        for (Boolean edgeWasCovered : wasEdgeCovered.values()){
            if (!edgeWasCovered){
                return false;
            }
        }
        //All edges were covered
        return  true;
    }

    private void addCall(Unit caller, SootMethod callee) {
        Edge edge = new Edge(getMethodOf(caller), (Stmt)caller, callee);
        demandDrivenCallGraph.addEdge(edge);
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

    protected void initializeUnitToOwner() {
        for (Iterator<MethodOrMethodContext> iter = Scene.v().getReachableMethods().listener(); iter.hasNext();) {
            SootMethod m = iter.next().method();
            initializeUnitToOwner(m);
        }
    }

    public void initializeUnitToOwner(SootMethod m) {
        if (m.hasActiveBody()) {
            Body b = m.getActiveBody();
            PatchingChain<Unit> units = b.getUnits();
            for (Unit unit : units) {
                unitToOwner.put(unit, b);
            }
        }
    }
}
