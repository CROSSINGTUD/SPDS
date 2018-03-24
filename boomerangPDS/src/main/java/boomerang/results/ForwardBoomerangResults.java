package boomerang.results;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import boomerang.stats.IBoomerangStats;
import heros.utilities.DefaultValueMap;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.nodes.INode;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.State;

public class ForwardBoomerangResults<W extends Weight> {

	private final ForwardQuery query;
	private final DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers;
	private final BiDiInterproceduralCFG<Unit, SootMethod> bwicfg;
	private final BiDiInterproceduralCFG<Unit, SootMethod> icfg;
	private final boolean timedout;
	private final IBoomerangStats<W> stats;
	private Stopwatch analysisWatch;

	public ForwardBoomerangResults(ForwardQuery query, boolean timedout, DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers, BiDiInterproceduralCFG<Unit, SootMethod> icfg, BiDiInterproceduralCFG<Unit, SootMethod> bwicfg, IBoomerangStats<W> stats, Stopwatch analysisWatch) {
		this.query = query;
		this.timedout = timedout;
		this.queryToSolvers = queryToSolvers;
		this.icfg = icfg;
		this.bwicfg = bwicfg;
		this.stats = stats;
		this.analysisWatch = analysisWatch;
	}
	
	public Stopwatch getAnalysisWatch() {
		return analysisWatch;
	}
	
	public boolean isTimedout() {
		return timedout;
	}
	
	public Table<Statement,Val, W> getResults(){
		final Table<Statement,Val, W> results = HashBasedTable.create();
		WeightedPAutomaton<Statement, INode<Val>, W> fieldAut = queryToSolvers.getOrCreate(query).getCallAutomaton();
		for(Entry<Transition<Statement, INode<Val>>, W> e : fieldAut.getTransitionsToFinalWeights().entrySet()){
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
		Table<Statement, Val, W> res = getResults();
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
				for(Unit callSite : icfg.getCallersOf(flowReaches)){
					SootMethod callee = icfg.getMethodOf(callSite);
					if(visitedMethods.contains(callee)){
						for(Entry<Val, W> valAndW : res.row(exitStmt).entrySet()){
							for(Unit retSite : icfg.getSuccsOf(callSite)){
								escapes.addAll(forwardSolver.computeReturnFlow(flowReaches, (Stmt) ep, valAndW.getKey(), (Stmt) callSite, (Stmt) retSite));
							}
						}
					}
				}
				if(escapes.isEmpty()){
					Map<Val, W> row = res.row(exitStmt);
					findLastUsage(exitStmt, row, destructingStatement,forwardSolver);
				}
			}
		}

		return destructingStatement;
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
}
