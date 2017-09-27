package test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import boomerang.jimple.Val;
import ideal.Analysis;
import soot.Body;
import soot.Local;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import test.ExpectedResults.InternalState;
import test.core.selfrunning.AbstractTestingFramework;
import test.core.selfrunning.ImprecisionException;
import typestate.ConcreteState;
import typestate.TransitionFunction;
import typestate.TypestateAnalysisProblem;
import typestate.TypestateChangeFunction;
import typestate.finiteautomata.State;

public abstract class IDEALTestingFramework extends AbstractTestingFramework{
	protected JimpleBasedInterproceduralCFG icfg;
	protected long analysisTime;
	protected TestingResultReporter<ConcreteState> testingResultReporter;

	protected abstract TypestateChangeFunction createTypestateChangeFunction();

	protected Analysis<TransitionFunction<State>> createAnalysis() {
		return new Analysis<TransitionFunction<State>>(new TypestateAnalysisProblem() {

			@Override
			public JimpleBasedInterproceduralCFG icfg() {
				return icfg;
			}


			@Override
			public TypestateChangeFunction createTypestateChangeFunction() {
				return IDEALTestingFramework.this.createTypestateChangeFunction();
			}


			@Override
			public boolean enableAliasing() {
				return false;
			}


			@Override
			public long analysisBudgetInSeconds() {
				return 0;
			}


			@Override
			public boolean enableNullPointOfAlias() {
				return false;
			}


			@Override
			public boolean enableStrongUpdates() {
				return false;
			}
		});
	}


	@Override
	protected SceneTransformer createAnalysisTransformer() throws ImprecisionException {
		return new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				icfg = new JimpleBasedInterproceduralCFG(true);
				Set<Assertion> expectedResults = parseExpectedQueryResults(sootTestMethod);
//				testingResultReporter = new TestingResultReporter(expectedResults);
				
				executeAnalysis();
				List<Assertion> unsound = Lists.newLinkedList();
				List<Assertion> imprecise = Lists.newLinkedList();
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
				if (!imprecise.isEmpty()) {
					throw new ImprecisionException("Imprecise results: " + imprecise);
				}
			}
		};
	}

	protected void executeAnalysis() {
		IDEALTestingFramework.this.createAnalysis().run();
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
		for (Unit callSite : icfg.getCallsFromWithin(m)) {
			for (SootMethod callee : icfg.getCalleesOfCallAt(callSite))
				parseExpectedQueryResults(callee, queries, visited);
		}
		for (Unit u : activeBody.getUnits()) {
			if (!(u instanceof Stmt))
				continue;

			Stmt stmt = (Stmt) u;
			if (!(stmt.containsInvokeExpr()))
				continue;
			InvokeExpr invokeExpr = stmt.getInvokeExpr();
			String invocationName = invokeExpr.getMethod().getName();
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

	/**
	 * The methods parameter describes the variable that a query is issued for.
	 * Note: We misuse the @Deprecated annotation to highlight the method in the
	 * Code.
	 */

	@Deprecated
	protected static void mayBeInErrorState(Object variable) {

	}

	@Deprecated
	protected static void mustBeInErrorState(Object variable) {

	}

	@Deprecated
	protected static void mayBeInAcceptingState(Object variable) {

	}

	@Deprecated
	protected void mustBeInAcceptingState(Object variable) {

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
