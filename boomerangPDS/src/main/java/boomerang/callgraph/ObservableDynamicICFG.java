package boomerang.callgraph;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.WeightedBoomerang;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.BackwardBoomerangResults;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import heros.DontSynchronize;
import heros.SynchronizedBy;
import heros.solver.IDESolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.exceptions.UnitThrowAnalysis;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import wpds.impl.Weight;

import java.util.*;

/**
 * An interprocedural control-flow graph, for which caller-callee edges can be observed using {@link CalleeListener} and
 * {@link CallerListener}. Used for demand-driven call graph generation.
 *
 *
 * Starts with an graph only containing intraprocedual edges and uses a precomputed call graph to derive callers.
 *
 * @author Melanie Bruns on 04.05.2018
 */
public class ObservableDynamicICFG<W extends Weight> implements ObservableICFG<Unit, SootMethod>{

    private static final Logger logger = LogManager.getLogger();

    private int numberOfEdgesTakenFromPrecomputedCallGraph=0;

    private CallGraph demandDrivenCallGraph = new CallGraph();
    private CallGraph precomputedCallGraph;
    private WeightedBoomerang<W> solver;
    private Set<SootMethod> methodsWithCallFlow = Sets.newHashSet();

    private HashSet<CalleeListener<Unit, SootMethod>> calleeListeners = new HashSet<>();
    private HashSet<CallerListener<Unit, SootMethod>> callerListeners = new HashSet<>();

    private final boolean enableExceptions;

    @DontSynchronize("written by single thread; read afterwards")
    private final Map<Unit,Body> unitToOwner = new HashMap<>();

    @SynchronizedBy("by use of synchronized LoadingCache class")
    private final LoadingCache<Body,DirectedGraph<Unit>> bodyToUnitGraph = IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Body,DirectedGraph<Unit>>() {
        @Override
        public DirectedGraph<Unit> load(Body body){
            return makeGraph(body);
        }
        private DirectedGraph<Unit> makeGraph(Body body) {
            return enableExceptions
                    ? new ExceptionalUnitGraph(body, UnitThrowAnalysis.v() ,true)
                    : new BriefUnitGraph(body);
        }
    });

    @SynchronizedBy("by use of synchronized LoadingCache class")
    private final LoadingCache<SootMethod,List<Value>> methodToParameterRefs = IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,List<Value>>() {
        @Override
        public List<Value> load(SootMethod m){
            return m.getActiveBody().getParameterRefs();
        }
    });

    @SynchronizedBy("by use of synchronized LoadingCache class")
    private final LoadingCache<SootMethod,Set<Unit>> methodToCallsFromWithin = IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,Set<Unit>>() {
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


    public ObservableDynamicICFG(WeightedBoomerang<W> solver){
        this(solver, true);
    }

    public ObservableDynamicICFG(WeightedBoomerang<W> solver, boolean enableExceptions) {
        this.solver = solver;
        this.enableExceptions = enableExceptions;

        this.precomputedCallGraph = Scene.v().getCallGraph();

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
        Stmt stmt = (Stmt) unit;
        Iterator<Edge> edgeIterator = demandDrivenCallGraph.edgesOutOf(unit);
        while (edgeIterator.hasNext()){
            Edge edge = edgeIterator.next();
            listener.onCalleeAdded(unit, edge.tgt());
        }

        InvokeExpr ie = stmt.getInvokeExpr();
        //Now check if we need to find new edges
        if ((ie instanceof InstanceInvokeExpr)){
            //If it was invoked on an object we might find new instances
            if (ie instanceof SpecialInvokeExpr){
                //If it was a special invoke, there is a single target
                addCallIfNotInGraph(unit, ie.getMethod(), Kind.SPECIAL);
                //If the precomputed graph has more edges than our graph, there may be more edges to find
            } else if (potentiallyHasMoreEdges(precomputedCallGraph.edgesOutOf(unit),
                    demandDrivenCallGraph.edgesOutOf(unit))){
                //Query for callees of the unit and add edges to the graph
                queryForCallees(unit);
            }
        } else {
            //Call was not invoked on an object. Must be static
            addCallIfNotInGraph(unit, ie.getMethod(), Kind.STATIC);
        }
    }

    private void queryForCallees(Unit unit) {
        //Construct BackwardQuery, so we know which types the object might have
        logger.debug("Queried for callees of '{}'.", unit);
        Stmt stmt = (Stmt) unit;
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        Value value = ((InstanceInvokeExpr) invokeExpr).getBase();
        Val val = new Val(value, getMethodOf(stmt));
        Statement statement = new Statement(stmt, getMethodOf(unit));
        BackwardQuery query = new BackwardQuery(statement, val);

        //Execute that query
        BackwardBoomerangResults<W> results = solver.solve(query);

        //Go through possible types an add edges to implementations in possible types
        for (ForwardQuery forwardQuery : results.getAllocationSites().keySet()){
            logger.debug("Found AllocationSite '{}'.", forwardQuery);
            Type type = forwardQuery.getType();
            if (type instanceof RefType){
                SootMethod calleeMethod = getMethodFromClassOrFromSuperclass(invokeExpr.getMethod(), ((RefType) type).getSootClass());
                if (calleeMethod != null)
                    addCallIfNotInGraph(unit, calleeMethod, Kind.VIRTUAL);
            } else if (type instanceof ArrayType){
                Type base = ((ArrayType) type).baseType;
                if (base instanceof RefType){
                    SootMethod calleeMethod = getMethodFromClassOrFromSuperclass(invokeExpr.getMethod(), ((RefType) base).getSootClass());
                    addCallIfNotInGraph(unit, calleeMethod, Kind.VIRTUAL);
                }
            }
        }
    }

    private SootMethod getMethodFromClassOrFromSuperclass(SootMethod method, SootClass sootClass){
        SootClass originalClass = sootClass;
        while(sootClass != null){
            for (SootMethod candidate : sootClass.getMethods()){
                if (candidate.getSubSignature().equals(method.getSubSignature())){
                    return candidate;
                }
            }
            if (sootClass.hasSuperclass()){
                sootClass = sootClass.getSuperclass();
            } else {
                logger.error("Did not find method {} for class {}", method, originalClass);
                return null;
            }
        }
        logger.error("Did not find method {} for class {}", method, originalClass);
        return null;
    }

    @Override
    public void addCallerListener(CallerListener<Unit, SootMethod> listener) {
        callerListeners.add(listener);

        SootMethod method = listener.getObservedCallee();

        logger.debug("Queried for callers of {}.", method);

        //Notify the new listener about what we already now
        Iterator<Edge> edgeIterator = demandDrivenCallGraph.edgesInto(method);
        while (edgeIterator.hasNext()){
            Edge edge = edgeIterator.next();
            listener.onCallerAdded(edge.srcUnit(), method);
        }
    }

    private boolean potentiallyHasMoreEdges(Iterator<Edge> chaEdgeIterator, Iterator<Edge> knownEdgeIterator){
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
                return true;
            }
        }
        //All edges were covered
        return false;
    }

    @Override
    public Collection<Unit> getAllPrecomputedCallers(SootMethod sootMethod) {
        logger.debug("Getting precomputed callers of {}", sootMethod);
        Set<Unit> callers = new HashSet<>();
        Iterator<Edge> precomputedCallers = precomputedCallGraph.edgesInto(sootMethod);
        while (precomputedCallers.hasNext()){
            Edge methodCall = precomputedCallers.next();
            callers.add(methodCall.srcUnit());
            boolean wasPrecomputedAdded = addCallIfNotInGraph(methodCall.srcUnit(), methodCall.tgt(), methodCall.kind());
            if (wasPrecomputedAdded)
                numberOfEdgesTakenFromPrecomputedCallGraph++;
        }
        return callers;
    }

    /**
     * Returns true if the call was added to the call graph, false if it was already present and the call graph did not
     * change
     */
    private boolean addCallIfNotInGraph(Unit caller, SootMethod callee, Kind kind) {
        if (isCallInGraph(caller, callee))
            return false;

        logger.debug("Added call from unit '{}' to method '{}'", caller, callee);
        Edge edge = new Edge(getMethodOf(caller), caller, callee, kind);
        demandDrivenCallGraph.addEdge(edge);
        //Notify all interested listeners, so ..
        //.. CalleeListeners interested in callees of the caller or the CallGraphExtractor that is interested in any
        for (CalleeListener<Unit, SootMethod> listener : Lists.newArrayList(calleeListeners)){
            if (caller.equals(listener.getObservedCaller()))
                listener.onCalleeAdded(caller, callee);
        }
        // .. CallerListeners interested in callers of the callee or the CallGraphExtractor that is interested in any
        for (CallerListener<Unit, SootMethod> listener : Lists.newArrayList(callerListeners)){
            if (callee.equals(listener.getObservedCallee()))
                listener.onCallerAdded(caller, callee);
        }
        return true;
    }

    private boolean isCallInGraph(Unit caller, SootMethod callee) {
        Iterator<Edge> edgesOutOfCaller = demandDrivenCallGraph.edgesOutOf(caller);
        while(edgesOutOfCaller.hasNext()){
            Edge edge = edgesOutOfCaller.next();
            if (edge.tgt().equals(callee)){
                return true;
            }
        }
        return false;
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

    private void initializeUnitToOwner() {
        for (Iterator<MethodOrMethodContext> iter = Scene.v().getReachableMethods().listener(); iter.hasNext();) {
            SootMethod m = iter.next().method();
            if (m.hasActiveBody()) {
                Body b = m.getActiveBody();
                PatchingChain<Unit> units = b.getUnits();
                for (Unit unit : units) {
                    unitToOwner.put(unit, b);
                }
            }
        }
    }

    public CallGraph getCallGraphCopy(){
        CallGraph copy = new CallGraph();
        for (Edge edge : demandDrivenCallGraph) {
            Edge edgeCopy = new Edge(edge.src(), edge.srcUnit(), edge.tgt(), edge.kind());
            copy.addEdge(edgeCopy);
        }
        return copy;
    }

	@Override
	public boolean isMethodsWithCallFlow(SootMethod method) {
		return methodsWithCallFlow.contains(method);
	}

	public void addMethodWithCallFlow(SootMethod method) {
        methodsWithCallFlow.add(method);
    }

    @Override
    public int getNumberOfEdgesTakenFromPrecomputedGraph() {
        return numberOfEdgesTakenFromPrecomputedCallGraph;
    }

    @Override
    public void resetCallGraph() {
        demandDrivenCallGraph = new CallGraph();
        numberOfEdgesTakenFromPrecomputedCallGraph = 0;
        methodsWithCallFlow.clear();
        calleeListeners.clear();
        callerListeners.clear();
    }

}
