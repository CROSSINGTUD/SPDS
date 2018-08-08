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
package test;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Lists;

import boomerang.WeightedForwardQuery;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.callgraph.ObservableStaticICFG;
import boomerang.debugger.CallGraphDebugger;
import boomerang.debugger.Debugger;
import boomerang.debugger.IDEVizDebugger;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.ForwardBoomerangResults;
import ideal.IDEALAnalysis;
import ideal.IDEALAnalysisDefinition;
import ideal.IDEALResultHandler;
import ideal.IDEALSeedSolver;
import ideal.StoreIDEALResultHandler;
import soot.Body;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sync.pds.solver.WeightFunctions;
import test.ExpectedResults.InternalState;
import test.core.selfrunning.AbstractTestingFramework;
import test.core.selfrunning.ImprecisionException;
import typestate.TransitionFunction;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;


public abstract class IDEALTestingFramework extends AbstractTestingFramework{
	protected ObservableICFG<Unit,SootMethod> staticIcfg;
	private static final boolean FAIL_ON_IMPRECISE = false;
	private static final boolean VISUALIZATION = false;

	protected StoreIDEALResultHandler<TransitionFunction> resultHandler = new StoreIDEALResultHandler<>();
	protected abstract TypeStateMachineWeightFunctions getStateMachine();

	protected IDEALAnalysis<TransitionFunction> createAnalysis() {
		return new IDEALAnalysis<TransitionFunction>(new IDEALAnalysisDefinition<TransitionFunction>() {

			@Override
			public Collection<WeightedForwardQuery<TransitionFunction>> generate(SootMethod method, Unit stmt, Collection<SootMethod> calledMethod) {
				return  IDEALTestingFramework.this.getStateMachine().generateSeed(method, stmt, calledMethod);
			}

			@Override
			public WeightFunctions<Statement, Val, Statement, TransitionFunction> weightFunctions() {
				return IDEALTestingFramework.this.getStateMachine();
			}
			
			@Override
			public Debugger<TransitionFunction> debugger(IDEALSeedSolver<TransitionFunction> solver) {
				return VISUALIZATION ? new IDEVizDebugger<>(new File(ideVizFile.getAbsolutePath().replace(".json", " " + solver.getSeed() +".json")),icfg) : new CallGraphDebugger(dotFile, icfg.getCallGraphCopy());
			}
			
			@Override
			public IDEALResultHandler<TransitionFunction> getResultHandler() {
				return resultHandler;
			}
			
		});
	}

	@Override
	protected SceneTransformer createAnalysisTransformer() throws ImprecisionException {
		return new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				staticIcfg = new ObservableStaticICFG(new JimpleBasedInterproceduralCFG());
				Set<Assertion> expectedResults = parseExpectedQueryResults(sootTestMethod);
				TestingResultReporter testingResultReporter = new TestingResultReporter(expectedResults);
				
				Map<WeightedForwardQuery<TransitionFunction>, ForwardBoomerangResults<TransitionFunction>> seedToSolvers = executeAnalysis();
				for(Entry<WeightedForwardQuery<TransitionFunction>, ForwardBoomerangResults<TransitionFunction>> e : seedToSolvers.entrySet()){
					testingResultReporter.onSeedFinished(e.getKey().asNode(), e.getValue());
				}
				List<Assertion> unsound = Lists.newLinkedList();
				List<Assertion> imprecise = Lists.newLinkedList();
				for (Assertion r : expectedResults){
					if (r instanceof ShouldNotBeAnalyzed){
						throw new RuntimeException(r.toString());
					}
				}
				for (Assertion r : expectedResults) {
					if (!r.isSatisfied()) {
						unsound.add(r);
					}
				}
				for (Assertion r : expectedResults) {
					if (r.isImprecise()) {
						imprecise.add(r);
					}
				}
				if (!unsound.isEmpty())
					throw new RuntimeException("Unsound results: " + unsound);
				if (!imprecise.isEmpty() && FAIL_ON_IMPRECISE) {
					throw new ImprecisionException("Imprecise results: " + imprecise);
				}
			}
		};
	}

	protected Map<WeightedForwardQuery<TransitionFunction>, ForwardBoomerangResults<TransitionFunction>> executeAnalysis() {
		IDEALTestingFramework.this.createAnalysis().run();
		return resultHandler.getResults();
	}

	private Set<Assertion> parseExpectedQueryResults(SootMethod sootTestMethod) {
		Set<Assertion> results = new HashSet<>();
		parseExpectedQueryResults(sootTestMethod, results, new HashSet<SootMethod>());
		return results;
	}

	private void parseExpectedQueryResults(SootMethod m, Set<Assertion> queries, Set<SootMethod> visited) {
		if (!m.hasActiveBody() || visited.contains(m))
			return;
		visited.add(m);
		Body activeBody = m.getActiveBody();
		for (Unit callSite : staticIcfg.getCallsFromWithin(m)) {
			staticIcfg.addCalleeListener(new ParseExpectedQueryResultCalleeListener(queries, visited, callSite));
		}
		for (Unit u : activeBody.getUnits()) {
			if (!(u instanceof Stmt))
				continue;

			Stmt stmt = (Stmt) u;
			if (!(stmt.containsInvokeExpr()))
				continue;
			InvokeExpr invokeExpr = stmt.getInvokeExpr();
			String invocationName = invokeExpr.getMethod().getName();
			if (invocationName.equals("shouldNotBeAnalyzed")){
				queries.add(new ShouldNotBeAnalyzed(stmt));
			}
			if (!invocationName.startsWith("mayBeIn") && !invocationName.startsWith("mustBeIn"))
				continue;
			Value param = invokeExpr.getArg(0);
			Val val = new Val(param, m);
			if (invocationName.startsWith("mayBeIn")) {
				if (invocationName.contains("Error"))
					queries.add(new MayBe(stmt, val, InternalState.ERROR));
				else
					queries.add(new MayBe(stmt, val, InternalState.ACCEPTING));
			} else if (invocationName.startsWith("mustBeIn")) {
				if (invocationName.contains("Error"))
					queries.add(new MustBe(stmt, val, InternalState.ERROR));
				else
					queries.add(new MustBe(stmt, val, InternalState.ACCEPTING));
			}
		}
	}

	private class ParseExpectedQueryResultCalleeListener implements CalleeListener<Unit, SootMethod>{
		Set<Assertion> queries;
		Set<SootMethod> visited;
		Unit callSite;

		ParseExpectedQueryResultCalleeListener(Set<Assertion> queries, Set<SootMethod> visited, Unit callSite){
			this.queries = queries;
			this.visited = visited;
			this.callSite = callSite;
		}

		@Override
		public Unit getObservedCaller() {
			return callSite;
		}

		@Override
		public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
			parseExpectedQueryResults(sootMethod, queries, visited);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ParseExpectedQueryResultCalleeListener that = (ParseExpectedQueryResultCalleeListener) o;
			return Objects.equals(queries, that.queries) &&
					Objects.equals(visited, that.visited) &&
					Objects.equals(callSite, that.callSite);
		}

		@Override
		public int hashCode() {
			return Objects.hash(queries, visited, callSite);
		}
	}

	/**
	 * The methods parameter describes the variable that a query is issued for.
	 * Note: We misuse the @Deprecated annotation to highlight the method in the
	 * Code.
	 */

	protected static void mayBeInErrorState(Object variable) {

	}

	protected static void mustBeInErrorState(Object variable) {

	}

	protected static void mayBeInAcceptingState(Object variable) {

	}

	protected static void mustBeInAcceptingState(Object variable) {
	}

	protected static void shouldNotBeAnalyzed(){
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
