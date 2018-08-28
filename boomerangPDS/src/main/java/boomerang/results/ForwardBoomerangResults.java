package boomerang.results;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.callgraph.CallerListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.Util;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import boomerang.stats.IBoomerangStats;
import com.google.common.base.Stopwatch;
import com.google.common.collect.*;
import heros.utilities.DefaultValueMap;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class ForwardBoomerangResults<W extends Weight> {

	private final ForwardQuery query;
	private final DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers;
	private final ObservableICFG<Unit, SootMethod> bwicfg;
	private final ObservableICFG<Unit, SootMethod> icfg;
	private final boolean timedout;
	private final IBoomerangStats<W> stats;
	private Stopwatch analysisWatch;
	private long maxMemory;

	public ForwardBoomerangResults(ForwardQuery query, boolean timedout, DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers, ObservableICFG<Unit, SootMethod> icfg, ObservableICFG<Unit, SootMethod> bwicfg, IBoomerangStats<W> stats, Stopwatch analysisWatch) {
		this.query = query;
		this.timedout = timedout;
		this.queryToSolvers = queryToSolvers;
		this.icfg = icfg;
		this.bwicfg = bwicfg;
		this.stats = stats;
		this.analysisWatch = analysisWatch;
		stats.terminated(query, this);
		this.maxMemory = Util.getReallyUsedMemory();
	}
	
	public Stopwatch getAnalysisWatch() {
		return analysisWatch;
	}
	
	public boolean isTimedout() {
		return timedout;
	}
	
	public Table<Statement,Val, W> asStatementValWeightTable(){
		final Table<Statement,Val, W> results = HashBasedTable.create();
		WeightedPAutomaton<Statement, INode<Val>, W> callAut = queryToSolvers.getOrCreate(query).getCallAutomaton();
		for(Entry<Transition<Statement, INode<Val>>, W> e : callAut.getTransitionsToFinalWeights().entrySet()){
			Transition<Statement, INode<Val>> t = e.getKey();
			W w = e.getValue();
			if(t.getLabel().equals(Statement.epsilon()))
				continue;
			if(t.getStart().fact().value() instanceof Local && !t.getLabel().getMethod().equals(t.getStart().fact().m()))
				continue;
			if(t.getLabel().getUnit().isPresent())
				results.put(t.getLabel(),t.getStart().fact(),w);
		}
		return results;
	}
	

	
	public Table<Statement, Val, W>  getObjectDestructingStatements() {
		AbstractBoomerangSolver<W> solver = queryToSolvers.get(query);
		if(solver == null)
			return HashBasedTable.create();
		Table<Statement, Val, W> res = asStatementValWeightTable();
		Set<SootMethod> visitedMethods = Sets.newHashSet();
		for(Statement s : res.rowKeySet()){
			visitedMethods.add(s.getMethod());
		}
		ForwardBoomerangSolver<W> forwardSolver = (ForwardBoomerangSolver) queryToSolvers.get(query);
		Table<Statement, Val, W> destructingStatement = HashBasedTable.create();
		for(SootMethod flowReaches : visitedMethods){
			for(Unit ep : icfg.getEndPointsOf(flowReaches)){
				Statement exitStmt = new Statement((Stmt) ep, flowReaches);
				Set<State> escapes = Sets.newHashSet();
				icfg.addCallerListener(new DestructingStatementCallerListener(flowReaches, visitedMethods, exitStmt,
						escapes, res, forwardSolver, ep));
				if(escapes.isEmpty()){
					Map<Val, W> row = res.row(exitStmt);
					findLastUsage(exitStmt, row, destructingStatement,forwardSolver);
				}
			}
		}

		return destructingStatement;
	}

	private class DestructingStatementCallerListener implements CallerListener<Unit,SootMethod>{
		SootMethod flowReaches;
		Set<SootMethod> visitedMethods;
		Statement exitStmt;
		Set<State> escapes;
		Table<Statement, Val, W> res;
		ForwardBoomerangSolver<W> forwardSolver;
		Unit ep;

		DestructingStatementCallerListener(SootMethod flowReaches, Set<SootMethod> visitedMethods, Statement exitStmt,
										   Set<State> escapes, Table<Statement, Val, W> res,
										   ForwardBoomerangSolver<W> forwardSolver, Unit ep){
			this.flowReaches = flowReaches;
			this.visitedMethods = visitedMethods;
			this.exitStmt = exitStmt;
			this.escapes = escapes;
			this.res = res;
			this.forwardSolver = forwardSolver;
			this.ep = ep;
		}

		@Override
		public SootMethod getObservedCallee() {
			return flowReaches;
		}

		@Override
		public void onCallerAdded(Unit unit, SootMethod sootMethod) {
			SootMethod callee = icfg.getMethodOf(unit);
			if(visitedMethods.contains(callee)){
				for(Entry<Val, W> valAndW : res.row(exitStmt).entrySet()){
					for(Unit retSite : icfg.getSuccsOf(unit)){
						escapes.addAll(forwardSolver.computeReturnFlow(flowReaches, (
								Stmt) ep, valAndW.getKey(), (Stmt) unit, (Stmt) retSite));
					}
				}
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			DestructingStatementCallerListener that = (DestructingStatementCallerListener) o;
			return Objects.equals(flowReaches, that.flowReaches) &&
					Objects.equals(visitedMethods, that.visitedMethods) &&
					Objects.equals(exitStmt, that.exitStmt) &&
					Objects.equals(escapes, that.escapes) &&
					Objects.equals(res, that.res) &&
					Objects.equals(forwardSolver, that.forwardSolver) &&
					Objects.equals(ep, that.ep);
		}

		@Override
		public int hashCode() {

			return Objects.hash(flowReaches, visitedMethods, exitStmt, escapes, res, forwardSolver, ep);
		}
	}


	private void findLastUsage(Statement exitStmt, Map<Val, W> row, Table<Statement, Val, W> destructingStatement, ForwardBoomerangSolver<W> forwardSolver) {
		LinkedList<Statement> worklist = Lists.newLinkedList();
		worklist.add(exitStmt);
		Set<Statement> visited = Sets.newHashSet();
		while(!worklist.isEmpty()){
			Statement curr = worklist.poll();
			if(!visited.add(curr)){
				continue;
			}
			boolean valueUsedInStmt = false;
			for(Entry<Val, W> e : row.entrySet()){
				if(forwardSolver.valueUsedInStatement(curr.getUnit().get(), e.getKey())){
					destructingStatement.put(curr, e.getKey(), e.getValue());
					valueUsedInStmt = true;
				}
			}
			if(!valueUsedInStmt){
				for(Unit succ : bwicfg.getSuccsOf(curr.getUnit().get())){
					worklist.add(new Statement((Stmt) succ, curr.getMethod()));
				}
			}
		}
	}

	public IBoomerangStats<W> getStats() {
		return stats;
	}

	public Map<Statement, SootMethod> getInvokedMethodOnInstance() {
		Map<Statement, SootMethod> invokedMethodsOnInstance = Maps.newHashMap();
		if(query.stmt().isCallsite()) {
			Stmt queryUnit = query.stmt().getUnit().get();
			if(queryUnit.containsInvokeExpr()) {
				invokedMethodsOnInstance.put(query.stmt(), queryUnit.getInvokeExpr().getMethod());
			}
		}
		queryToSolvers.get(query).getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, W>() {

			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
					WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
				if(!t.getLabel().equals(Field.empty()) || t.getStart() instanceof GeneratedState) {
					return;
				}
				Node<Statement, Val> node = t.getStart().fact();
				Val fact = node.fact();
				Statement curr = node.stmt();
				if(curr.isCallsite()){
					Stmt callSite = (Stmt) curr.getUnit().get();
					if(callSite.getInvokeExpr() instanceof InstanceInvokeExpr){
						InstanceInvokeExpr e = (InstanceInvokeExpr)callSite.getInvokeExpr();
						if(e.getBase().equals(fact.value())){
							invokedMethodsOnInstance.put(curr, e.getMethod());
						}
					}
				}
			}
		});
		return invokedMethodsOnInstance;
	}
	
	public boolean containsCallRecursion() {
		for(Entry<Query, AbstractBoomerangSolver<W>> e: queryToSolvers.entrySet()) {
			if(e.getValue().getCallAutomaton().containsLoop()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containsFieldLoop() {
		for(Entry<Query, AbstractBoomerangSolver<W>> e: queryToSolvers.entrySet()) {
			if(e.getValue().getFieldAutomaton().containsLoop()) {
				return true;
			}
		}
		return false;
	}

	public long getMaxMemory() {
		return maxMemory;
	}
}
