/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package test.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.IntAndStringBoomerangOptions;
import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.WholeProgramBoomerang;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableDynamicICFG;
import boomerang.callgraph.ObservableICFG;
import boomerang.callgraph.ObservableStaticICFG;
import boomerang.debugger.CallGraphDebugger;
import boomerang.debugger.Debugger;
import boomerang.debugger.IDEVizDebugger;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.preanalysis.PreTransformBodies;
import boomerang.results.BackwardBoomerangResults;
import boomerang.seedfactory.SimpleSeedFactory;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.stats.IBoomerangStats;
import boomerang.util.AccessPath;
import boomerang.util.AccessPathParser;
import heros.utilities.DefaultValueMap;
import soot.Body;
import soot.Local;
import soot.PackManager;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.JastAddJ.VariableScope;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractBoomerangTest extends AbstractTestingFramework {

	private static final boolean FAIL_ON_IMPRECISE = false;
	private static final boolean VISUALIZATION = false;

	private ObservableICFG<Unit, SootMethod> dynamicIcfg;
	private ObservableStaticICFG staticIcfg;
	private QueryForCallSiteDetector queryDetector;
	private Collection<? extends Query> allocationSites;
	protected Collection<? extends Query> queryForCallSites;
	protected Collection<Error> unsoundErrors = Sets.newHashSet();
	protected Collection<Error> imprecisionErrors = Sets.newHashSet();


	protected int analysisTimeout = 3000 *1000;

	public enum AnalysisMode {
		WholeProgram, DemandDrivenBackward;
	}

	protected AnalysisMode[] getAnalyses() {
		return new AnalysisMode[] {
				AnalysisMode.WholeProgram,
				AnalysisMode.DemandDrivenBackward
				};
	}

	protected SceneTransformer createAnalysisTransformer() {
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.prepare", new PreTransformBodies()));
		return new SceneTransformer() {

			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {

				staticIcfg = new ObservableStaticICFG(new JimpleBasedInterproceduralCFG());
				queryDetector = new QueryForCallSiteDetector(staticIcfg);
				queryForCallSites = queryDetector.computeSeeds();


				if(queryDetector.integerQueries){
					allocationSites = extractQuery(new IntegerAllocationSiteOf());
				} else{
					allocationSites = extractQuery(new AllocationSiteOf());
				}
				for(int i = 0; i< getIterations();i++) {
					for (AnalysisMode analysis : getAnalyses()) {
						switch (analysis) {
						case WholeProgram:
							if(!queryDetector.integerQueries)
								runWholeProgram();
							break;
						case DemandDrivenBackward:
							runDemandDrivenBackward();
							break;
						}
					}
					if(queryDetector.resultsMustNotBeEmpty)
						return;
					if (!unsoundErrors.isEmpty()) {
						throw new RuntimeException(Joiner.on("\n").join(unsoundErrors));
					}
					if (!imprecisionErrors.isEmpty() && FAIL_ON_IMPRECISE) {
						throw new AssertionError(Joiner.on("\n").join(imprecisionErrors));
					}
				}
			}
		};
	}

	public int getIterations() {
		return 1;
	}

	private void runDemandDrivenBackward() {
		// Run backward analysis
		Set<Node<Statement, Val>> backwardResults = runQuery(queryForCallSites);
		compareQuery(allocationSites, backwardResults, AnalysisMode.DemandDrivenBackward);
	}

	private class AllocationSiteOf implements ValueOfInterestInUnit {
		public Optional<? extends Query> test(Stmt unit) {
			if (unit instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) unit;
				if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof NewExpr) {
					NewExpr expr = ((NewExpr) as.getRightOp());
					if (allocatesObjectOfInterest(expr)) {
						Local local = (Local) as.getLeftOp();
						Statement statement = new Statement(unit, staticIcfg.getMethodOf(unit));
						ForwardQuery forwardQuery = new ForwardQuery(statement, new AllocVal(local, staticIcfg.getMethodOf(unit), as.getRightOp(),statement));
						return Optional.<Query>of(forwardQuery);
					}
				}
			}
			return Optional.empty();
		}
	}
	private class IntegerAllocationSiteOf implements ValueOfInterestInUnit {
		public Optional<? extends Query> test(Stmt stmt) {
			if (stmt instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) stmt;
				if (as.getLeftOp().toString().equals("allocation")) {
					Statement statement = new Statement(stmt, staticIcfg.getMethodOf(stmt));
					if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof IntConstant) {
						Local local = (Local) as.getLeftOp();
						ForwardQuery forwardQuery = new ForwardQuery(statement,
                                new AllocVal(local, staticIcfg.getMethodOf(stmt), as.getRightOp(),new Statement(as,
                                        staticIcfg.getMethodOf(stmt))));
						return Optional.<Query>of(forwardQuery);
					}

					if(as.containsInvokeExpr()){
						AtomicReference<Query> returnValue = new AtomicReference<>();
						staticIcfg.addCalleeListener(new IntegerAllocationSiteCalleeListener(returnValue, as, statement, stmt));
						if (returnValue.get() != null){
							return Optional.of(returnValue.get());
						}
					}
				}
			}

			return Optional.empty();
		}
	}

	private class IntegerAllocationSiteCalleeListener implements CalleeListener<Unit, SootMethod>{
		private AtomicReference<Query> p_returnValue;
		private AssignStmt p_as;
		private Statement p_statement;
		private Stmt p_stmt;

		IntegerAllocationSiteCalleeListener(AtomicReference<Query> returnValue, AssignStmt as, Statement statement, Stmt stmt){
			p_returnValue = returnValue;
			p_as = as;
			p_statement = statement;
			p_stmt = stmt;
		}

		@Override
		public Unit getObservedCaller() {
			return p_as;
		}

		@Override
		public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
			for(Unit u : staticIcfg.getEndPointsOf(sootMethod)){
				if(u instanceof ReturnStmt && ((ReturnStmt) u).getOp() instanceof IntConstant){
					ForwardQuery forwardQuery = new ForwardQuery(p_statement,
							new AllocVal(p_as.getLeftOp(), staticIcfg.getMethodOf(p_stmt), ((ReturnStmt) u).getOp(), new Statement((Stmt) u, sootMethod)));
					p_returnValue.set(forwardQuery);
				}
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			IntegerAllocationSiteCalleeListener that = (IntegerAllocationSiteCalleeListener) o;
			return Objects.equals(p_returnValue, that.p_returnValue) &&
					Objects.equals(p_as, that.p_as) &&
					Objects.equals(p_statement, that.p_statement) &&
					Objects.equals(p_stmt, that.p_stmt);
		}

		@Override
		public int hashCode() {

			return Objects.hash(p_returnValue, p_as, p_statement, p_stmt);
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
		
		if(queryDetector.resultsMustNotBeEmpty && results.isEmpty()){
			throw new RuntimeException("Expected some results, but Boomerang returned no allocation sites.");
		}
	}

	private Set<Node<Statement, Val>> runQuery(Collection<? extends Query> queries) {
		final Set<Node<Statement, Val>> results = Sets.newHashSet();
		
		for (final Query query : queries) {
			DefaultBoomerangOptions options = (queryDetector.integerQueries ? new IntAndStringBoomerangOptions() : new DefaultBoomerangOptions(){
				@Override
				public boolean arrayFlows() {
					return true;
				}

				@Override
				public int analysisTimeoutMS() {
					return analysisTimeout;
				}

				@Override
				public boolean onTheFlyCallGraph() {
					return false;
				}
			});
			Boomerang solver = new Boomerang(options) {
				@Override
				public ObservableICFG<Unit, SootMethod> icfg() {
					if (dynamicIcfg == null){
						dynamicIcfg = new ObservableDynamicICFG<>(this);
					}
					return dynamicIcfg;
				}
				
				@Override
				public Debugger createDebugger() {
					return VISUALIZATION ? new IDEVizDebugger(ideVizFile, icfg()) :
							new CallGraphDebugger(dotFile, dynamicIcfg.getCallGraphCopy());
				}

				@Override
				public SimpleSeedFactory getSeedFactory() {
					return null;
				}
			};
			if(query instanceof BackwardQuery){
				setupSolver(solver);
				BackwardBoomerangResults<NoWeight> res = solver.solve((BackwardQuery) query);

				solver.debugOutput();

				for(ForwardQuery q : res.getAllocationSites().keySet()){
					results.add(q.asNode());

					for (Node<Statement, Val> s : solver.getSolvers().get(q).getReachedStates()) {
						if (s.stmt().getMethod().toString().contains("unreachable")) {
							throw new RuntimeException("Propagation within unreachable method found.");
						}
					}
				}

//				System.out.println(res.getAllAliases());
				if(queryDetector.accessPathQuery){
					checkContainsAllExpectedAccessPath(res.getAllAliases());
				}
			}
		}
		return results;
	}

	private void checkContainsAllExpectedAccessPath(Set<AccessPath> allAliases) {
		HashSet<AccessPath> expected = Sets.newHashSet(queryDetector.expectedAccessPaths);
		expected.removeAll(allAliases);
		if(!expected.isEmpty()){
			throw new RuntimeException("Did not find all access path! " +expected);
		}
	}

	private void runWholeProgram() {
		final Set<Node<Statement, Val>> results = Sets.newHashSet();
		WholeProgramBoomerang<NoWeight> solver = new WholeProgramBoomerang<NoWeight>(new DefaultBoomerangOptions() {
			@Override
			public int analysisTimeoutMS() {
				return analysisTimeout;
			}
		}) {
			@Override
			public ObservableICFG<Unit, SootMethod> icfg() {
				return staticIcfg;
			}

			@Override
			public Debugger createDebugger() {
				return VISUALIZATION ? new IDEVizDebugger(ideVizFile, staticIcfg) : new CallGraphDebugger(dotFile, staticIcfg.getCallGraphCopy());
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
		setupSolver(solver);
		solver.wholeProgramAnalysis();
		DefaultValueMap<Query, AbstractBoomerangSolver<NoWeight>> solvers = solver.getSolvers();
		for (final Query q : solvers.keySet()) {
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

	protected void setupSolver(WeightedBoomerang<NoWeight> solver) {
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
		extractQuery(sootTestMethod, predicate, queries, null, new HashSet<>());
		return queries;
	}

	private void extractQuery(SootMethod m, ValueOfInterestInUnit predicate, Collection<Query> queries, Stmt callSite,
			Set<Node<SootMethod, Stmt>> visited) {
		if (!m.hasActiveBody() || visited.contains(new Node<>(m, callSite)))
			return;
		visited.add(new Node<>(m, callSite));
		Body activeBody = m.getActiveBody();
		for (Unit cs : staticIcfg.getCallsFromWithin(m)) {
		    staticIcfg.addCalleeListener(new ExtractQueryCalleeListener(cs, predicate, queries, callSite, visited));
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

	private class ExtractQueryCalleeListener implements CalleeListener<Unit,SootMethod> {
		Unit p_cs;
		ValueOfInterestInUnit p_predicate;
		Collection<Query> p_queries;
		Stmt p_callsite;
		Set<Node<SootMethod, Stmt>> p_visited;

		ExtractQueryCalleeListener(Unit cs, ValueOfInterestInUnit predicate, Collection<Query> queries, Stmt callsite,
								   Set<Node<SootMethod, Stmt>> visited){
			this.p_cs=cs;
			this.p_predicate=predicate;
			this.p_queries=queries;
			this.p_callsite=callsite;
			this.p_visited=visited;
		}

		public Unit getObservedCaller() {
			return p_cs;
		}

		public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
			extractQuery(sootMethod, p_predicate, p_queries, (p_callsite == null ? (Stmt) p_cs : p_callsite), p_visited);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ExtractQueryCalleeListener that = (ExtractQueryCalleeListener) o;
			return Objects.equals(p_cs, that.p_cs) &&
					Objects.equals(p_predicate, that.p_predicate) &&
					Objects.equals(p_queries, that.p_queries) &&
					Objects.equals(p_callsite, that.p_callsite) &&
					Objects.equals(p_visited, that.p_visited);
		}

		@Override
		public int hashCode() {

			return Objects.hash(p_cs, p_predicate, p_queries, p_callsite, p_visited);
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

	public static void queryFor(Object variable) {

	}

	public static void accessPathQueryFor(Object variable, String aliases) {

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

}
