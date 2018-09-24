package boomerang.results;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.callgraph.CallerListener;
import boomerang.callgraph.ObservableDynamicICFG;
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
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.PAutomaton;
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

public class ForwardBoomerangResults<W extends Weight> extends AbstractBoomerangResults<W> {

	private final ForwardQuery query;
	private final boolean timedout;
	private final IBoomerangStats<W> stats;
	private Stopwatch analysisWatch;
	private long maxMemory;
	private ObservableICFG<Unit,SootMethod> icfg;

	public ForwardBoomerangResults(ForwardQuery query, ObservableICFG<Unit,SootMethod> icfg, boolean timedout,
			DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers,
			IBoomerangStats<W> stats, Stopwatch analysisWatch) {
		super(queryToSolvers);
		this.query = query;
		this.icfg = icfg;
		this.timedout = timedout;
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

	public Table<Statement, Val, W> asStatementValWeightTable() {
		final Table<Statement, Val, W> results = HashBasedTable.create();
		WeightedPAutomaton<Statement, INode<Val>, W> callAut = queryToSolvers.getOrCreate(query).getCallAutomaton();
		for (Entry<Transition<Statement, INode<Val>>, W> e : callAut.getTransitionsToFinalWeights().entrySet()) {
			Transition<Statement, INode<Val>> t = e.getKey();
			W w = e.getValue();
			if (t.getLabel().equals(Statement.epsilon()))
				continue;
			if (t.getStart().fact().value() instanceof Local
					&& !t.getLabel().getMethod().equals(t.getStart().fact().m()))
				continue;
			if (t.getLabel().getUnit().isPresent())
				results.put(t.getLabel(), t.getStart().fact(), w);
		}
		return results;
	}

	public Table<Statement, Val, W> getObjectDestructingStatements() {
		AbstractBoomerangSolver<W> solver = queryToSolvers.get(query);
		if (solver == null)
			return HashBasedTable.create();
		Table<Statement, Val, W> res = asStatementValWeightTable();
		Set<SootMethod> visitedMethods = Sets.newHashSet();
		for (Statement s : res.rowKeySet()) {
			visitedMethods.add(s.getMethod());
		}
		ForwardBoomerangSolver<W> forwardSolver = (ForwardBoomerangSolver) queryToSolvers.get(query);
		Table<Statement, Val, W> destructingStatement = HashBasedTable.create();
		for (SootMethod flowReaches : visitedMethods) {
			for (Unit ep : icfg.getEndPointsOf(flowReaches)) {
				Statement exitStmt = new Statement((Stmt) ep, flowReaches);
				Set<State> escapes = Sets.newHashSet();
				icfg.addCallerListener(new CallerListener<Unit, SootMethod>() {
					@Override
					public SootMethod getObservedCallee() {
						return flowReaches;
					}

					@Override
					public void onCallerAdded(Unit callSite, SootMethod m) {
						SootMethod callee = icfg.getMethodOf(callSite);
						if (visitedMethods.contains(callee)) {
							for (Entry<Val, W> valAndW : res.row(exitStmt).entrySet()) {
								for (Unit retSite : icfg.getSuccsOf(callSite)) {
									escapes.addAll(forwardSolver.computeReturnFlow(flowReaches, (Stmt) ep, valAndW.getKey(),
											(Stmt) callSite, (Stmt) retSite));
								}
							}
						}
					}
				});
				if (escapes.isEmpty()) {
					Map<Val, W> row = res.row(exitStmt);
					findLastUsage(exitStmt, row, destructingStatement, forwardSolver);
				}
			}
		}

		return destructingStatement;
	}

	private void findLastUsage(Statement exitStmt, Map<Val, W> row, Table<Statement, Val, W> destructingStatement,
			ForwardBoomerangSolver<W> forwardSolver) {
		LinkedList<Statement> worklist = Lists.newLinkedList();
		worklist.add(exitStmt);
		Set<Statement> visited = Sets.newHashSet();
		while (!worklist.isEmpty()) {
			Statement curr = worklist.poll();
			if (!visited.add(curr)) {
				continue;
			}
			boolean valueUsedInStmt = false;
			for (Entry<Val, W> e : row.entrySet()) {
				if (forwardSolver.valueUsedInStatement(curr.getUnit().get(), e.getKey())) {
					destructingStatement.put(curr, e.getKey(), e.getValue());
					valueUsedInStmt = true;
				}
			}
			if (!valueUsedInStmt) {
				for (Unit succ : icfg.getPredsOf(curr.getUnit().get())) {
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
		if (query.stmt().isCallsite()) {
			Stmt queryUnit = query.stmt().getUnit().get();
			if (queryUnit.containsInvokeExpr()) {
				invokedMethodsOnInstance.put(query.stmt(), queryUnit.getInvokeExpr().getMethod());
			}
		}
		queryToSolvers.get(query).getFieldAutomaton()
				.registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {

					@Override
					public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
							WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
						if (!t.getLabel().equals(Field.empty()) || t.getStart() instanceof GeneratedState) {
							return;
						}
						Node<Statement, Val> node = t.getStart().fact();
						Val fact = node.fact();
						Statement curr = node.stmt();
						if (curr.isCallsite()) {
							Stmt callSite = (Stmt) curr.getUnit().get();
							if (callSite.getInvokeExpr() instanceof InstanceInvokeExpr) {
								InstanceInvokeExpr e = (InstanceInvokeExpr) callSite.getInvokeExpr();
								if (e.getBase().equals(fact.value())) {
									invokedMethodsOnInstance.put(curr, e.getMethod());
								}
							}
						}
					}
				});
		return invokedMethodsOnInstance;
	}

	public Map<Node<Statement, Val>, AbstractBoomerangResults<W>.Context> getPotentialNullPointerDereferences() {
		Set<Node<Statement, Val>> res = Sets.newHashSet();
		queryToSolvers.get(query).getFieldAutomaton()
				.registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {

					@Override
					public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
							WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
						if (!t.getLabel().equals(Field.empty()) || t.getStart() instanceof GeneratedState) {
							return;
						}
						Node<Statement, Val> node = t.getStart().fact();
						Val fact = node.fact();
						Statement curr = node.stmt();
						if (curr.isCallsite()) {
							Stmt callSite = (Stmt) curr.getUnit().get();
							if (callSite.getInvokeExpr() instanceof InstanceInvokeExpr) {
								InstanceInvokeExpr e = (InstanceInvokeExpr) callSite.getInvokeExpr();
								if (e.getBase().equals(fact.value())) {
									res.add(node);
								}
							}
						}
						if (curr.getUnit().get() instanceof AssignStmt) {
							AssignStmt assignStmt = (AssignStmt) curr.getUnit().get();
							if (assignStmt.getRightOp() instanceof InstanceFieldRef) {
								InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getRightOp();
								if (ifr.getBase().equals(fact.value())) {
									res.add(node);
								}
							}
							if (assignStmt.getRightOp() instanceof LengthExpr) {
								LengthExpr lengthExpr = (LengthExpr) assignStmt.getRightOp();
								if (lengthExpr.getOp().equals(fact.value())) {
									res.add(node);
								}
							}
						}

					}
				});

		Map<Node<Statement, Val>, AbstractBoomerangResults<W>.Context> resWithContext = Maps.newHashMap();
		for (Node<Statement, Val> r : res) {
			AbstractBoomerangResults<W>.Context context = constructContextGraph(query, r);
			resWithContext.put(r, context);
		}
		return resWithContext;
	}
	
	public Context getContext(Node<Statement,Val> node) {
		return constructContextGraph(query, node);
	}

	public boolean containsCallRecursion() {
		for (Entry<Query, AbstractBoomerangSolver<W>> e : queryToSolvers.entrySet()) {
			if (e.getValue().getCallAutomaton().containsLoop()) {
				return true;
			}
		}
		return false;
	}

	public boolean containsFieldLoop() {
		for (Entry<Query, AbstractBoomerangSolver<W>> e : queryToSolvers.entrySet()) {
			if (e.getValue().getFieldAutomaton().containsLoop()) {
				return true;
			}
		}
		return false;
	}

	public long getMaxMemory() {
		return maxMemory;
	}
	
}
