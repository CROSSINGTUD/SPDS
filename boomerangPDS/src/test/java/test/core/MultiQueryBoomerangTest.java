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
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Rule;
import org.junit.rules.Timeout;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.callgraph.ObservableStaticICFG;
import boomerang.debugger.Debugger;
import boomerang.debugger.IDEVizDebugger;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.BackwardBoomerangResults;
import boomerang.seedfactory.SeedFactory;
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
import soot.jimple.ClassConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sync.pds.solver.nodes.Node;
import test.core.selfrunning.AbstractTestingFramework;
import wpds.impl.Weight.NoWeight;

public class MultiQueryBoomerangTest extends AbstractTestingFramework {

	private static final boolean FAIL_ON_IMPRECISE = false;

	@Rule
	public Timeout timeout = new Timeout(10000000);
	private ObservableICFG<Unit, SootMethod> icfg;
	private Collection<? extends Query> allocationSites;
	protected Collection<? extends Query> queryForCallSites;
	protected Multimap<Query,Query> expectedAllocsForQuery = HashMultimap.create();
	protected Collection<Error> unsoundErrors = Sets.newHashSet();
	protected Collection<Error> imprecisionErrors = Sets.newHashSet();
	private SeedFactory<NoWeight> seedFactory;

	protected int analysisTimeout = 300 *1000;

	private WeightedBoomerang<NoWeight> solver;

	protected SceneTransformer createAnalysisTransformer() {
		return new SceneTransformer() {

			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				icfg = new ObservableStaticICFG(new JimpleBasedInterproceduralCFG());
				seedFactory = new SeedFactory<NoWeight>(){


					@Override
					public ObservableICFG<Unit, SootMethod> icfg() {
						return icfg;
					}

					@Override
					protected Collection<? extends Query> generate(SootMethod method, Stmt u, Collection calledMethods) {
						Optional<? extends Query> query = new FirstArgumentOf("queryFor.*").test(u);

						if(query.isPresent()){
							ClassConstant arg = (ClassConstant) u.getInvokeExpr().getArg(1);
							expectedAllocsForQuery.putAll(query.get(), extractQuery(new AllocationSiteOf(arg.toSootType().toString())));
							return Collections.singleton(query.get());
						}
						return Collections.emptySet();
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
		public Optional<? extends Query> test(Stmt unit) {
			if (unit instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) unit;
				if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof NewExpr) {
					NewExpr expr = ((NewExpr) as.getRightOp());
					System.out.println(as + type);
					if (allocatesObjectOfInterest(expr, type)) {
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
		};
		solver = new Boomerang(options) {
			@Override
			public ObservableICFG<Unit, SootMethod> icfg() {
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
		if (!m.hasActiveBody() || visited.contains(new Node<SootMethod, Stmt>(m, callSite)))
			return;
		visited.add(new Node<SootMethod, Stmt>(m, callSite));
		Body activeBody = m.getActiveBody();
		for (Unit cs : icfg.getCallsFromWithin(m)) {
			icfg.addCalleeListener((CalleeListener<Unit, SootMethod>) (unit, sootMethod) -> {
				if (unit.equals(cs)){
					extractQuery(sootMethod, predicate, queries, (callSite == null ? (Stmt) cs : callSite), visited);
				}
			});
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
