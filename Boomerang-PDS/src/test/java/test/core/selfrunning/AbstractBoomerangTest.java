package test.core.selfrunning;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import boomerang.AliasFinder;
import boomerang.AliasResults;
import boomerang.BoomerangOptions;
import boomerang.Query;
import boomerang.accessgraph.AccessGraph;
import boomerang.cfg.ExtendedICFG;
import boomerang.cfg.IExtendedICFG;
import boomerang.context.AllCallersRequester;
import boomerang.context.IContextRequester;
import boomerang.context.NoContextRequester;
import boomerang.debug.DefaultBoomerangDebugger;
import boomerang.debug.IBoomerangDebugger;
import boomerang.debug.JSONOutputDebugger;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.IPropagationController;
import heros.solver.Pair;
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
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class AbstractBoomerangTest extends AbstractTestingFramework{
	private IExtendedICFG icfg;
	private IContextRequester contextReuqester;
	private BoomerangOptions options;

	private boolean useIDEViz() {
		return !getTestCaseClassName().contains("LongTest");
	}

	protected SceneTransformer createAnalysisTransformer() {
		return new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				icfg = new ExtendedICFG(new JimpleBasedInterproceduralCFG(true));
				AbstractBoomerangTest.this.options = new BoomerangOptions(){
							@Override
							public IBoomerangDebugger getDebugger() {
								return (useIDEViz() ?  new JSONOutputDebugger(ideVizFile) : new DefaultBoomerangDebugger());
							}

							@Override
							public IExtendedICFG icfg() {
								return icfg;
							}
							@Override
							public long getTimeBudget() {
								return TimeUnit.SECONDS.toMillis(600);
							}
							
							
				};
				Query q = parseQuery();
				contextReuqester = (q.getMethod().getName().toString().equals(testMethodName.getMethodName()) ? new NoContextRequester()
						: new AllCallersRequester());

				AliasResults expectedResults = parseExpectedQueryResults(q);
				AliasResults results = runQuery(q);
				compareQuery(q, expectedResults, results);
			}
		};
	}

	private void compareQuery(Query q, AliasResults expectedResults, AliasResults results) {
		System.out.println("Boomerang Allocations Sites: " + results.keySet());
		System.out.println("Boomerang Results: " + results);
		System.out.println("Expected Results: " + expectedResults);
		Set<Pair<Unit, AccessGraph>> falseNegativeAllocationSites = new HashSet<>(expectedResults.keySet());
		falseNegativeAllocationSites.removeAll(results.keySet());
		Set<Pair<Unit, AccessGraph>> falsePositiveAllocationSites = new HashSet<>(results.keySet());
		falsePositiveAllocationSites.removeAll(expectedResults.keySet());
		String answer = "Query: " + q.getAp() + "@" + q.getStmt() + "€" + q.getMethod() + " \n"
				+ (falseNegativeAllocationSites.isEmpty() ? "" : "\nFN:" + falseNegativeAllocationSites)
				+ (falsePositiveAllocationSites.isEmpty() ? "" : "\nFP:" + falsePositiveAllocationSites + "\n");
		if (!falseNegativeAllocationSites.isEmpty()) {
			throw new RuntimeException("Unsound results for:" + answer);
		}
		Set<String> aliasVariables = new HashSet<>();
		for (AccessGraph g : expectedResults.values()) {
			aliasVariables.add(g.toString());
		}
		for (AccessGraph remove : results.values())
			aliasVariables.remove(remove.toString());
		HashSet<String> missingVariables = new HashSet<>();

		for (String g : aliasVariables) {
			if (g.contains("alias") && !g.contains("**"))
				missingVariables.add(g);
		}
		if (!missingVariables.isEmpty())
			throw new RuntimeException("Unsound, missed variables " + missingVariables);
		if (!falsePositiveAllocationSites.isEmpty())
			throw new ImprecisionException("Imprecise results: " + answer);
	}

	private AliasResults runQuery(Query q) {
		AliasFinder boomerang = new AliasFinder(options);
		boomerang.startQuery();
		AliasResults query = boomerang.findAliasAtStmt(q.getAp(), q.getStmt(), contextReuqester).withoutNullAllocationSites();
		for(String methodSignature : errorOnVisitMethod()){
			if(boomerang.context.getOptions().onTheFlyCallGraphGeneration() && boomerang.context.visitableMethod(Scene.v().getMethod(methodSignature))){
				throw new RuntimeException("Analysis visited method " + methodSignature);
			}
		}
		
		return query;
	}

	private AliasResults parseExpectedQueryResults(Query q) {
		Set<Pair<Unit, AccessGraph>> allocationSiteWithCallStack = parseAllocationSitesWithCallStack();
		Set<Local> aliasedVariables = parseAliasedVariables();
		aliasedVariables.add(q.getAp().getBase());
		AliasResults expectedResults = associateVariableAliasesToAllocationSites(allocationSiteWithCallStack,
				aliasedVariables);

		return expectedResults;
	}

	private AliasResults associateVariableAliasesToAllocationSites(
			Set<Pair<Unit, AccessGraph>> allocationSiteWithCallStack, Set<Local> aliasedVariables) {
		AliasResults res = new AliasResults();
		for (Pair<Unit, AccessGraph> allocatedVariableWithStack : allocationSiteWithCallStack) {
			for (Local l : aliasedVariables) {
				res.put(allocatedVariableWithStack, new AccessGraph(l));
			}
		}
		return res;
	}

	private Set<Local> parseAliasedVariables() {
		Set<Local> out = new HashSet<>();
		Body activeBody = sootTestMethod.getActiveBody();
		for (Unit u : activeBody.getUnits()) {
			if (!(u instanceof AssignStmt))
				continue;
			AssignStmt assignStmt = (AssignStmt) u;
			if (!(assignStmt.getLeftOp() instanceof Local))
				continue;
			if (!assignStmt.getLeftOp().toString().contains("alias")
					&& !assignStmt.getLeftOp().toString().contains("query"))
				continue;
			Local aliasedVar = (Local) assignStmt.getLeftOp();
			out.add(aliasedVar);
		}
		return out;
	}

	private Set<Pair<Unit, AccessGraph>> parseAllocationSitesWithCallStack() {
		Set<Unit> callsites = icfg.getCallsFromWithin(sootTestMethod);
		Set<Pair<Unit, AccessGraph>> out = new HashSet<>();
		for (Unit call : callsites) {
			for (AccessGraph accessGraphAtAllocationSite : transitivelyReachableAllocationSite(call,
					new HashSet<SootMethod>())) {
				out.add(new Pair<Unit, AccessGraph>(call, accessGraphAtAllocationSite));
			}
		}
		for (Unit u : sootTestMethod.getActiveBody().getUnits()) {
			if (!(u instanceof AssignStmt))
				continue;
			AssignStmt as = (AssignStmt) u;

			if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof NewExpr) {
				NewExpr expr = ((NewExpr) as.getRightOp());
				if (allocatesObjectOfInterest(expr)) {
					Local local = (Local) as.getLeftOp();
					AccessGraph accessGraph = new AccessGraph(local);
					out.add(new Pair<Unit, AccessGraph>(as, accessGraph.deriveWithAllocationSite(as, expr.getType(),true)));
				}
			}

		}
		return out;
	}

	private boolean allocatesObjectOfInterest(NewExpr rightOp) {
		SootClass interfaceType = Scene.v().getSootClass("test.core.selfrunning.AllocatedObject");
		if (!interfaceType.isInterface())
			return false;
		RefType allocatedType = rightOp.getBaseType();
		return Scene.v().getActiveHierarchy().getImplementersOf(interfaceType).contains(allocatedType.getSootClass());
	}

	private Set<AccessGraph> transitivelyReachableAllocationSite(Unit call, Set<SootMethod> visited) {
		Set<AccessGraph> out = new HashSet<>();
		for (SootMethod m : icfg.getCalleesOfCallAt(call)) {
			if (visited.contains(m))
				continue;
			visited.add(m);
			if (!m.hasActiveBody())
				continue;
			for (Unit u : m.getActiveBody().getUnits()) {
				if (!(u instanceof AssignStmt))
					continue;
				AssignStmt as = (AssignStmt) u;

				if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof NewExpr) {
					NewExpr expr = ((NewExpr) as.getRightOp());
					if (allocatesObjectOfInterest(expr)) {
						Local local = (Local) as.getLeftOp();
						AccessGraph accessGraph = new AccessGraph(local);
						out.add(accessGraph.deriveWithAllocationSite(as, expr.getType(), true));
					}
				}

			}
			for (Unit u : icfg.getCallsFromWithin(m))
				out.addAll(transitivelyReachableAllocationSite(u, visited));
		}
		return out;
	}

	private Query parseQuery() {
		LinkedList<Query> queries = new LinkedList<>();
		parseQuery(sootTestMethod, queries, new HashSet<SootMethod>());
		if (queries.size() == 0)
			throw new RuntimeException("No call to method queryFor was found within " + sootTestMethod.getName());
		if (queries.size() > 1)
			System.err.println(
					"More than one possible query found, might be unambigious, picking query " + queries.getLast());
		return queries.getLast();
	}

	private void parseQuery(SootMethod m, LinkedList<Query> queries, Set<SootMethod> visited) {
		if (!m.hasActiveBody() || visited.contains(m))
			return;
		visited.add(m);
		Body activeBody = m.getActiveBody();
		for (Unit callSite : icfg.getCallsFromWithin(m)) {
			for (SootMethod callee : icfg.getCalleesOfCallAt(callSite))
				parseQuery(callee, queries, visited);
		}
		for (Unit u : activeBody.getUnits()) {
			if (!(u instanceof Stmt))
				continue;

			Stmt stmt = (Stmt) u;
			if (!(stmt.containsInvokeExpr()))
				continue;
			InvokeExpr invokeExpr = stmt.getInvokeExpr();
			if (!invokeExpr.getMethod().getName().equals("queryFor"))
				continue;
			Value param = invokeExpr.getArg(0);
			if (!(param instanceof Local))
				continue;
			Local queryVar = (Local) param;
			queries.add(new Query(new AccessGraph(queryVar), stmt, m));
		}
	}

	private String getTestCaseClassName() {
		return this.getClass().getName().replace("class ", "");
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
