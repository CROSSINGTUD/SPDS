package test.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.rules.Timeout;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.IntAndStringBoomerangOptions;
import boomerang.Query;
import boomerang.WholeProgramBoomerang;
import boomerang.debugger.Debugger;
import boomerang.debugger.IDEVizDebugger;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.seedfactory.SeedFactory;
import boomerang.solver.AbstractBoomerangSolver;
import heros.utilities.DefaultValueMap;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import test.core.selfrunning.AbstractTestingFramework;
import wpds.impl.Transition;
import wpds.impl.Weight.NoWeight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;

public class AbstractBoomerangTest extends AbstractTestingFramework {

	private static final boolean FAIL_ON_IMPRECISE = false;

	@Rule
	public Timeout timeout = new Timeout(10000000);
	private JimpleBasedInterproceduralCFG icfg;
	private Collection<? extends Query> allocationSites;
	protected Collection<? extends Query> queryForCallSites;
	protected Collection<Error> unsoundErrors = Sets.newHashSet();
	protected Collection<Error> imprecisionErrors = Sets.newHashSet();
	protected boolean resultsMustNotBeEmpty = false;

	private boolean integerQueries;
	private SeedFactory<NoWeight> seedFactory;

	private enum AnalysisMode {
		WholeProgram, DemandDrivenForward, DemandDrivenBackward;
	}

	protected AnalysisMode[] getAnalyses() {
		return new AnalysisMode[] {
				 AnalysisMode.WholeProgram,
//				 AnalysisMode.DemandDrivenForward,
				AnalysisMode.DemandDrivenBackward
				};
	}

	protected SceneTransformer createAnalysisTransformer() {
		return new SceneTransformer() {

			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				icfg = new JimpleBasedInterproceduralCFG(true);
				seedFactory = new SeedFactory<NoWeight>(){
					@Override
					public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
						return icfg;
					}

					@Override
					protected Collection<? extends Query> generate(SootMethod method, Stmt u, Collection calledMethods) {
						Optional<? extends Query> query = new FirstArgumentOf("queryFor").test(u);

						if(query.isPresent()){
							return Collections.singleton(query.get());
						}
						query = new FirstArgumentOf("queryForAndNotEmpty").test(u);

						if(query.isPresent()){
							resultsMustNotBeEmpty = true;
							return Collections.singleton(query.get());
						}
						query = new FirstArgumentOf("intQueryFor").test(u);
						if(query.isPresent()){
							integerQueries = true;
							return Collections.singleton(query.get());
						}
						return Collections.emptySet();
					}
				};
				queryForCallSites = seedFactory.computeSeeds();
				if(integerQueries){
					allocationSites = extractQuery(new IntegerAllocationSiteOf());
				} else{
					allocationSites = extractQuery(new AllocationSiteOf());
				}
				for (AnalysisMode analysis : getAnalyses()) {
					switch (analysis) {
					case WholeProgram:
						if(!integerQueries)
							runWholeProgram();
						break;
					case DemandDrivenBackward:
						runDemandDrivenBackward();
						break;
					case DemandDrivenForward:
						runDemandDrivenForward();
						break;
					}
				}
				if(resultsMustNotBeEmpty)
					return;
				if (!unsoundErrors.isEmpty()) {
					throw new RuntimeException(Joiner.on("\n").join(unsoundErrors));
				}
				if (!imprecisionErrors.isEmpty() && FAIL_ON_IMPRECISE) {
					throw new AssertionError(Joiner.on("\n").join(imprecisionErrors));
				}
			}
		};
	}

	private void runDemandDrivenBackward() {
		// Run backward analysis
		if (queryForCallSites.size() > 1)
			throw new RuntimeException("Found more than one backward query to execute!");
		Set<Node<Statement, Val>> backwardResults = runQuery(queryForCallSites);
		compareQuery(allocationSites, backwardResults, AnalysisMode.DemandDrivenBackward);
	}

	private void runDemandDrivenForward() {
		// Run forward analysis
		Set<Node<Statement, Val>> results = runQuery(allocationSites);
		compareQuery(queryForCallSites, results, AnalysisMode.DemandDrivenForward);
	}

	private class AllocationSiteOf implements ValueOfInterestInUnit {
		public Optional<? extends Query> test(Stmt unit) {
			if (unit instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) unit;
				if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof NewExpr) {
					NewExpr expr = ((NewExpr) as.getRightOp());
					if (allocatesObjectOfInterest(expr)) {
						Local local = (Local) as.getLeftOp();
						Statement statement = new Statement(unit, icfg.getMethodOf(unit));
						ForwardQuery forwardQuery = new ForwardQuery(statement, new AllocVal(local, icfg.getMethodOf(unit), as.getRightOp()));
						return Optional.<Query>of(forwardQuery);
					}
				}
			}
			return Optional.absent();
		}
	}
	private class IntegerAllocationSiteOf implements ValueOfInterestInUnit {
		public Optional<? extends Query> test(Stmt unit) {
			if (unit instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) unit;
				if (as.getLeftOp().toString().equals("allocation")) {
					Statement statement = new Statement(unit, icfg.getMethodOf(unit));
					if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof IntConstant) {
						Local local = (Local) as.getLeftOp();
						ForwardQuery forwardQuery = new ForwardQuery(statement, new AllocVal(local, icfg.getMethodOf(unit), as.getRightOp()));
						return Optional.<Query>of(forwardQuery);
					}

					if(as.containsInvokeExpr()){
						for(SootMethod m : icfg.getCalleesOfCallAt(as)){
							for(Unit u : icfg.getEndPointsOf(m)){
								if(u instanceof ReturnStmt && ((ReturnStmt) u).getOp() instanceof IntConstant){
									ForwardQuery forwardQuery = new ForwardQuery(statement, new AllocVal(as.getLeftOp(), icfg.getMethodOf(unit), ((ReturnStmt) u).getOp()));
									return Optional.<Query>of(forwardQuery);
								}
							}
						}
					}
				}
			}
			
			return Optional.absent();
		}
	}
	private class FirstArgumentOf implements ValueOfInterestInUnit {

		private String methodNameMatcher;

		public FirstArgumentOf(String methodNameMatcher) {
			this.methodNameMatcher = methodNameMatcher;
		}

		@Override
		public Optional<? extends Query> test(Stmt unit) {
			Stmt stmt = (Stmt) unit;
			if (!(stmt.containsInvokeExpr()))
				return Optional.absent();
			InvokeExpr invokeExpr = stmt.getInvokeExpr();
			if (!invokeExpr.getMethod().getName().matches(methodNameMatcher))
				return Optional.absent();
			Value param = invokeExpr.getArg(0);
			if (!(param instanceof Local))
				return Optional.absent();
			return Optional.<Query>of(new BackwardQuery(new Statement(unit, icfg.getMethodOf(unit)),
					new Val(param, icfg.getMethodOf(unit))));
		}
	}

	private void compareQuery(Collection<? extends Query> expectedResults,
			Collection<? extends Node<Statement, Val>> results, AnalysisMode analysis) {
		System.out.println("Boomerang Results: " + results);
		System.out.println("Expected Results: " + expectedResults);
		Collection<Node<Statement, Val>> falseNegativeAllocationSites = new HashSet<>();
		for (Query res : expectedResults) {
			if (!results.contains(res.asNode()))
				falseNegativeAllocationSites.add(res.asNode());
		}
		Collection<? extends Node<Statement, Val>> falsePositiveAllocationSites = new HashSet<>(results);
		for (Query res : expectedResults) {
			falsePositiveAllocationSites.remove(res.asNode());
		}

		String answer = (falseNegativeAllocationSites.isEmpty() ? "" : "\nFN:" + falseNegativeAllocationSites)
				+ (falsePositiveAllocationSites.isEmpty() ? "" : "\nFP:" + falsePositiveAllocationSites + "\n");
		if (!falseNegativeAllocationSites.isEmpty()) {
			unsoundErrors.add(new Error(analysis + " Unsound results for:" + answer));
		}
		if (!falsePositiveAllocationSites.isEmpty())
			imprecisionErrors.add(new Error(analysis + " Imprecise results for:" + answer));
		
		if(resultsMustNotBeEmpty && results.isEmpty()){
			throw new RuntimeException("Expected some results, but Boomerang returned no allocation sites.");
		}
	}

	private Set<Node<Statement, Val>> runQuery(Collection<? extends Query> queries) {
		final Set<Node<Statement, Val>> results = Sets.newHashSet();
		for (final Query query : queries) {
			DefaultBoomerangOptions options = (integerQueries ? new IntAndStringBoomerangOptions() : new DefaultBoomerangOptions(){
				@Override
				public boolean arrayFlows() {
					return true;
				}
			});
			Boomerang solver = new Boomerang(options) {
				@Override
				public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
					return icfg;
				}
				
				@Override
				public Debugger createDebugger() {
					return new IDEVizDebugger(ideVizFile,icfg);
				}

				@Override
				public SeedFactory<NoWeight> getSeedFactory() {
					return seedFactory;
				}
			};
			if(query instanceof BackwardQuery){
				Stopwatch watch = Stopwatch.createStarted();
				solver.solve(query);
				System.out.println("Test ("+sootTestMethod+" took: " + watch.elapsed());
				for(ForwardQuery q : solver.getAllocationSites((BackwardQuery) query)){
					results.add(q.asNode());
				}
                        

			}else{
				solver.solve(query);
				for(Node<Statement, Val> s : solver.getForwardReachableStates()){
					if(s.stmt().getUnit().isPresent()){
						Stmt stmt = s.stmt().getUnit().get();
						if(stmt.toString().contains("queryFor")){
							if(stmt.containsInvokeExpr()){
								InvokeExpr invokeExpr = stmt.getInvokeExpr();
								if(invokeExpr.getArg(0).equals(s.fact().value()))
									results.add(s);
							}
						}
					}
				}
			}
			solver.debugOutput();
			
		}
		return results;
	}

	private void runWholeProgram() {
		final Set<Node<Statement, Val>> results = Sets.newHashSet();
		WholeProgramBoomerang<NoWeight> solver = new WholeProgramBoomerang<NoWeight>() {
			@Override
			public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
				return icfg;
			}

			@Override
			public Debugger createDebugger() {
				return new IDEVizDebugger(ideVizFile, icfg);
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, NoWeight> getForwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, NoWeight>(NoWeight.NO_WEIGHT_ZERO,
						NoWeight.NO_WEIGHT_ONE);
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, NoWeight> getBackwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, NoWeight>(NoWeight.NO_WEIGHT_ZERO,
						NoWeight.NO_WEIGHT_ONE);
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, NoWeight> getBackwardCallWeights() {
				return new OneWeightFunctions<Statement, Val, Statement, NoWeight>(NoWeight.NO_WEIGHT_ZERO,
						NoWeight.NO_WEIGHT_ONE);
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, NoWeight> getForwardCallWeights(ForwardQuery sourceQuery) {
				return new OneWeightFunctions<Statement, Val, Statement, NoWeight>(NoWeight.NO_WEIGHT_ZERO,
						NoWeight.NO_WEIGHT_ONE);
			}

		};
		solver.wholeProgramAnalysis();
		DefaultValueMap<Query, AbstractBoomerangSolver<NoWeight>> solvers = solver.getSolvers();
		for (final Query q : solvers.keySet()) {
			if (!(q instanceof ForwardQuery))
				throw new RuntimeException(
						"Unexpected solver found, whole program analysis should only trigger forward queries");
			for (final Query queryForCallSite : queryForCallSites) {
				solvers.get(q).getFieldAutomaton()
						.registerListener(new WPAStateListener<Field, INode<Node<Statement, Val>>, NoWeight>(
								new SingleNode<Node<Statement, Val>>(queryForCallSite.asNode())) {

							@Override
							public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t,
									NoWeight w,
									WeightedPAutomaton<Field, INode<Node<Statement, Val>>, NoWeight> weightedPAutomaton) {
								if (t.getLabel().equals(Field.empty()) && t.getTarget().fact().equals(q.asNode())) {
									results.add(q.asNode());
								}
							}

							@Override
							public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t,
									NoWeight w,
									WeightedPAutomaton<Field, INode<Node<Statement, Val>>, NoWeight> weightedPAutomaton) {

							}
						});
			}
			for (Node<Statement, Val> s : solvers.get(q).getReachedStates()) {
				if (s.stmt().getMethod().toString().contains("unreachable")
						&& !q.toString().contains("dummyClass.main")) {
					throw new RuntimeException("Propagation within unreachable method found: " + q);
				}
			}
		}

		solver.debugOutput();
		compareQuery(allocationSites, results, AnalysisMode.WholeProgram);
		System.out.println();
	}

	private boolean allocatesObjectOfInterest(NewExpr rightOp) {
		SootClass interfaceType = Scene.v().getSootClass("test.core.selfrunning.AllocatedObject");
		if (!interfaceType.isInterface())
			return false;
		RefType allocatedType = rightOp.getBaseType();
		return Scene.v().getActiveHierarchy().getImplementersOf(interfaceType).contains(allocatedType.getSootClass());
	}

	private Collection<? extends Query> extractQuery(ValueOfInterestInUnit predicate) {
		Set<Query> queries = Sets.newHashSet();
		extractQuery(sootTestMethod, predicate, queries, null, new HashSet<Node<SootMethod, Stmt>>());
		return queries;
	}

	private void extractQuery(SootMethod m, ValueOfInterestInUnit predicate, Collection<Query> queries, Stmt callSite,
			Set<Node<SootMethod, Stmt>> visited) {
		if (!m.hasActiveBody() || visited.contains(new Node<SootMethod, Stmt>(m, callSite)))
			return;
		visited.add(new Node<SootMethod, Stmt>(m, callSite));
		Body activeBody = m.getActiveBody();
		for (Unit cs : icfg.getCallsFromWithin(m)) {
			for (SootMethod callee : icfg.getCalleesOfCallAt(cs))
				extractQuery(callee, predicate, queries, (callSite == null ? (Stmt) cs : callSite), visited);
		}
		for (Unit u : activeBody.getUnits()) {
			if (!(u instanceof Stmt))
				continue;
			Optional<? extends Query> optOfVal = predicate.test((Stmt) u);
			if (optOfVal.isPresent()) {
				queries.add(optOfVal.get());
			}
		}
	}

	protected Collection<String> errorOnVisitMethod() {
		return Lists.newLinkedList();
	}

	protected boolean includeJDK() {
		return true;
	}

	/**
	 * The methods parameter describes the variable that a query is issued for.
	 * Note: We misuse the @Deprecated annotation to highlight the method in the
	 * Code.
	 */

	protected void queryFor(Object variable) {

	}

	protected void queryForAndNotEmpty(Object variable) {

	}
	protected void intQueryFor(int variable) {

	}

	/**
	 * A call to this method flags the object as at the call statement as not
	 * reachable by the analysis.
	 * 
	 * @param variable
	 */
	protected void unreachable(Object variable) {

	}

	/**
	 * This method can be used in test cases to create branching. It is not
	 * optimized away.
	 * 
	 * @return
	 */
	protected boolean staticallyUnknown() {
		return true;
	}

	private interface ValueOfInterestInUnit {
		Optional<? extends Query> test(Stmt unit);
	}
}
