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

import boomerang.*;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableDynamicICFG;
import boomerang.callgraph.ObservableICFG;
import boomerang.callgraph.ObservableStaticICFG;
import boomerang.debugger.Debugger;
import boomerang.debugger.IDEVizDebugger;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.preanalysis.BoomerangPretransformer;
import boomerang.results.BackwardBoomerangResults;
import boomerang.seedfactory.SeedFactory;
import boomerang.seedfactory.SimpleSeedFactory;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.rules.Timeout;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sync.pds.solver.nodes.Node;
import test.core.selfrunning.AbstractTestingFramework;
import wpds.impl.Weight;
import wpds.impl.Weight.NoWeight;

import java.util.*;
import java.util.Map.Entry;

public class MultiQueryBoomerangTest extends AbstractTestingFramework {

	private static final boolean FAIL_ON_IMPRECISE = false;

	@Rule
	public Timeout timeout = new Timeout(10000000);
	private ObservableICFG<Unit, SootMethod> dynamicIcfg;
	private ObservableStaticICFG staticIcfg;
	private Collection<? extends Query> allocationSites;
	protected Collection<? extends Query> queryForCallSites;
	protected Multimap<Query,Query> expectedAllocsForQuery = HashMultimap.create();
	protected Collection<Error> unsoundErrors = Sets.newHashSet();
	protected Collection<Error> imprecisionErrors = Sets.newHashSet();
	private SeedFactory<Weight.NoWeight> seedFactory;

	protected int analysisTimeout = 300 *1000;

	private WeightedBoomerang<NoWeight> solver;

	protected SceneTransformer createAnalysisTransformer() {
		return new SceneTransformer() {

			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				BoomerangPretransformer.v().apply();
				staticIcfg = new ObservableStaticICFG(new JimpleBasedInterproceduralCFG());
				seedFactory = new SeedFactory<Weight.NoWeight>() {

					@Override
					protected Collection<? extends Query> generate(SootMethod method, Stmt u, Collection<SootMethod> calledMethods) {
						Optional<Query> query = new FirstArgumentOf("queryFor.*").test(u);

						if(query.isPresent()){
							ClassConstant arg = (ClassConstant) u.getInvokeExpr().getArg(1);
							expectedAllocsForQuery.putAll(query.get(), extractQuery(new AllocationSiteOf(arg.toSootType().toString())));
							return Collections.singleton(query.get());
						}
						return Collections.emptySet();
					}

					@Override
					public ObservableICFG<Unit, SootMethod> icfg() {
						return staticIcfg;
					}

				};
				queryForCallSites = seedFactory.computeSeeds();
				runDemandDrivenBackward();
				if (!unsoundErrors.isEmpty()) {
					throw new RuntimeException(Joiner.on("\n").join(unsoundErrors));
				}
				if (!imprecisionErrors.isEmpty() && FAIL_ON_IMPRECISE) {
					throw new AssertionError(Joiner.on("\n").join(imprecisionErrors));
				}
			}
		};
	}


	private class AllocationSiteOf implements ValueOfInterestInUnit {
		private String type;
		public AllocationSiteOf(String type) {
			this.type = type;
		}
		public Optional<Query> test(Stmt unit) {
			if (unit instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) unit;
				if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof NewExpr) {
					NewExpr expr = ((NewExpr) as.getRightOp());
					System.out.println(as + type);
					if (allocatesObjectOfInterest(expr, type)) {
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
	private class FirstArgumentOf implements ValueOfInterestInUnit {

		private String methodNameMatcher;

		public FirstArgumentOf(String methodNameMatcher) {
			this.methodNameMatcher = methodNameMatcher;
		}

		@Override
		public Optional<Query> test(Stmt unit) {
			Stmt stmt = unit;
			if (!(stmt.containsInvokeExpr()))
				return Optional.empty();
			InvokeExpr invokeExpr = stmt.getInvokeExpr();
			if (!invokeExpr.getMethod().getName().matches(methodNameMatcher))
				return Optional.empty();
			Value param = invokeExpr.getArg(0);
			if (!(param instanceof Local))
				return Optional.empty();
			return Optional.of(new BackwardQuery(new Statement(unit, staticIcfg.getMethodOf(unit)),
					new Val(param, staticIcfg.getMethodOf(unit))));
		}
	}

	private void compareQuery(Query query,
			Collection<? extends Query> results) {
		Collection<Query> expectedResults = expectedAllocsForQuery.get(query);
		System.out.println("Boomerang Results: " + results);
		System.out.println("Expected Results: " + expectedResults);
		Collection<Query> falseNegativeAllocationSites = new HashSet<>();
		for (Query res : expectedResults) {
			if (!results.contains(res))
				falseNegativeAllocationSites.add(res);
		}
		Collection<Query> falsePositiveAllocationSites = new HashSet<>(results);
		for (Query res : expectedResults) {
			falsePositiveAllocationSites.remove(res);
		}

		String answer = (falseNegativeAllocationSites.isEmpty() ? "" : "\nFN:" + falseNegativeAllocationSites)
				+ (falsePositiveAllocationSites.isEmpty() ? "" : "\nFP:" + falsePositiveAllocationSites + "\n");
		if (!falseNegativeAllocationSites.isEmpty()) {
			unsoundErrors.add(new Error(" Unsound results for:" + answer));
		}
		if (!falsePositiveAllocationSites.isEmpty())
			imprecisionErrors.add(new Error( " Imprecise results for:" + answer));
		for(Entry<Query, Query> e : expectedAllocsForQuery.entries()) {
			if(!e.getKey().equals(query)) {
				if(results.contains(e.getValue())) {
					solver.debugOutput();
					throw new RuntimeException("A query contains the result of a different query.\n"+query + " \n contains \n" + e.getValue());
				}
			}
			
		}
	}

	private void runDemandDrivenBackward() {
		DefaultBoomerangOptions options = new DefaultBoomerangOptions(){
			@Override
			public boolean arrayFlows() {
				return true;
			}
			
			@Override
			public int analysisTimeoutMS() {
				return analysisTimeout;
			}

			@Override
			public boolean onTheFlyCallGraph(){
				return false;
			}
		};
		solver = new Boomerang(options) {
			@Override
			public ObservableICFG<Unit, SootMethod> icfg() {
				if (dynamicIcfg == null){
					dynamicIcfg = new ObservableDynamicICFG<>(this);
				}
				return dynamicIcfg;
			}
			
			@Override
			public Debugger createDebugger() {
				return new IDEVizDebugger(ideVizFile, icfg());
			}

			@Override
			public SeedFactory<Weight.NoWeight> getSeedFactory() {
				return seedFactory;
			}
		};
		for (final Query query : queryForCallSites) {
			if(query instanceof BackwardQuery){
				BackwardBoomerangResults<NoWeight> res = solver.solve((BackwardQuery) query);
				compareQuery(query, res.getAllocationSites().keySet());
			}
		}
		solver.debugOutput();
	}

	private boolean allocatesObjectOfInterest(NewExpr rightOp, String type) {
		SootClass interfaceType = Scene.v().getSootClass(type);
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
			Optional<Query> optOfVal = predicate.test((Stmt) u);
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

	public static void queryFor1(Object variable, Class interfaceType) {

	}
	public static void queryFor2(Object variable, Class interfaceType) {

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
	 */
	protected void unreachable(Object variable) {

	}

	/**
	 * This method can be used in test cases to create branching. It is not
	 * optimized away.
	 *
	 */
	protected boolean staticallyUnknown() {
		return true;
	}

	private interface ValueOfInterestInUnit {
		Optional<Query> test(Stmt unit);
	}
}
