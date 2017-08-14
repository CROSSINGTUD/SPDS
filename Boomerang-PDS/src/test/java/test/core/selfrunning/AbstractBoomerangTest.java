package test.core.selfrunning;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Lists;

import analysis.Node;
import boomerang.jimple.Statement;
import boomerang.solver.BoomerangSolver;
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
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class AbstractBoomerangTest extends AbstractTestingFramework{
	private JimpleBasedInterproceduralCFG icfg;

	private boolean useIDEViz() {
		return !getTestCaseClassName().contains("LongTest");
	}

	protected SceneTransformer createAnalysisTransformer() {
		return new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				icfg = new JimpleBasedInterproceduralCFG(true);
				List<Node<Statement, Value>> q = parseQuery();

//				Collection<Node<Statement,Value>> expectedResults = parseExpectedQueryResults(q);
				Collection<Node<Statement,Value>> results = runQuery(q);
//				compareQuery(q, expectedResults, results);
			}
		};
	}

//	private void compareQuery(Query q, AliasResults expectedResults, AliasResults results) {
//		System.out.println("Boomerang Allocations Sites: " + results.keySet());
//		System.out.println("Boomerang Results: " + results);
//		System.out.println("Expected Results: " + expectedResults);
//		Set<Pair<Unit, AccessGraph>> falseNegativeAllocationSites = new HashSet<>(expectedResults.keySet());
//		falseNegativeAllocationSites.removeAll(results.keySet());
//		Set<Pair<Unit, AccessGraph>> falsePositiveAllocationSites = new HashSet<>(results.keySet());
//		falsePositiveAllocationSites.removeAll(expectedResults.keySet());
//		String answer = "Query: " + q.getAp() + "@" + q.getStmt() + "€" + q.getMethod() + " \n"
//				+ (falseNegativeAllocationSites.isEmpty() ? "" : "\nFN:" + falseNegativeAllocationSites)
//				+ (falsePositiveAllocationSites.isEmpty() ? "" : "\nFP:" + falsePositiveAllocationSites + "\n");
//		if (!falseNegativeAllocationSites.isEmpty()) {
//			throw new RuntimeException("Unsound results for:" + answer);
//		}
//		Set<String> aliasVariables = new HashSet<>();
//		for (AccessGraph g : expectedResults.values()) {
//			aliasVariables.add(g.toString());
//		}
//		for (AccessGraph remove : results.values())
//			aliasVariables.remove(remove.toString());
//		HashSet<String> missingVariables = new HashSet<>();
//
//		for (String g : aliasVariables) {
//			if (g.contains("alias") && !g.contains("**"))
//				missingVariables.add(g);
//		}
//		if (!missingVariables.isEmpty())
//			throw new RuntimeException("Unsound, missed variables " + missingVariables);
//		if (!falsePositiveAllocationSites.isEmpty())
//			throw new ImprecisionException("Imprecise results: " + answer);
//	}

	private Set<Node<Statement, Value>> runQuery(Collection<Node<Statement, Value>> queries) {
		Set<Node<Statement,Value>> results = Sets.newHashSet();
		for(Node<Statement, Value> query : queries){
			BoomerangSolver solver = new BoomerangSolver(icfg);
			solver.solve(new Node<Statement, Value>(query.stmt(),query.fact()));
			results.addAll(solver.getReachedStates());
		}
		return results;
	}

//	private AliasResults parseExpectedQueryResults(Query q) {
//		Set<Pair<Unit, AccessGraph>> allocationSiteWithCallStack = parseAllocationSitesWithCallStack();
//		Set<Local> aliasedVariables = parseAliasedVariables();
//		aliasedVariables.add(q.getAp().getBase());
//		AliasResults expectedResults = associateVariableAliasesToAllocationSites(allocationSiteWithCallStack,
//				aliasedVariables);
//
//		return expectedResults;
//	}
//
//	private AliasResults associateVariableAliasesToAllocationSites(
//			Set<Pair<Unit, AccessGraph>> allocationSiteWithCallStack, Set<Local> aliasedVariables) {
//		AliasResults res = new AliasResults();
//		for (Pair<Unit, AccessGraph> allocatedVariableWithStack : allocationSiteWithCallStack) {
//			for (Local l : aliasedVariables) {
//				res.put(allocatedVariableWithStack, new AccessGraph(l));
//			}
//		}
//		return res;
//	}
//
//	private Set<Local> parseAliasedVariables() {
//		Set<Local> out = new HashSet<>();
//		Body activeBody = sootTestMethod.getActiveBody();
//		for (Unit u : activeBody.getUnits()) {
//			if (!(u instanceof AssignStmt))
//				continue;
//			AssignStmt assignStmt = (AssignStmt) u;
//			if (!(assignStmt.getLeftOp() instanceof Local))
//				continue;
//			if (!assignStmt.getLeftOp().toString().contains("alias")
//					&& !assignStmt.getLeftOp().toString().contains("query"))
//				continue;
//			Local aliasedVar = (Local) assignStmt.getLeftOp();
//			out.add(aliasedVar);
//		}
//		return out;
//	}
//
//	private Set<Pair<Unit, AccessGraph>> parseAllocationSitesWithCallStack() {
//		Set<Unit> callsites = icfg.getCallsFromWithin(sootTestMethod);
//		Set<Pair<Unit, AccessGraph>> out = new HashSet<>();
//		for (Unit call : callsites) {
//			for (AccessGraph accessGraphAtAllocationSite : transitivelyReachableAllocationSite(call,
//					new HashSet<SootMethod>())) {
//				out.add(new Pair<Unit, AccessGraph>(call, accessGraphAtAllocationSite));
//			}
//		}
//		for (Unit u : sootTestMethod.getActiveBody().getUnits()) {
//			if (!(u instanceof AssignStmt))
//				continue;
//			AssignStmt as = (AssignStmt) u;
//
//			if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof NewExpr) {
//				NewExpr expr = ((NewExpr) as.getRightOp());
//				if (allocatesObjectOfInterest(expr)) {
//					Local local = (Local) as.getLeftOp();
//					AccessGraph accessGraph = new AccessGraph(local);
//					out.add(new Pair<Unit, AccessGraph>(as, accessGraph.deriveWithAllocationSite(as, expr.getType(),true)));
//				}
//			}
//
//		}
//		return out;
//	}

	private boolean allocatesObjectOfInterest(NewExpr rightOp) {
		SootClass interfaceType = Scene.v().getSootClass("test.core.selfrunning.AllocatedObject");
		if (!interfaceType.isInterface())
			return false;
		RefType allocatedType = rightOp.getBaseType();
		return Scene.v().getActiveHierarchy().getImplementersOf(interfaceType).contains(allocatedType.getSootClass());
	}

//	private Set<AccessGraph> transitivelyReachableAllocationSite(Unit call, Set<SootMethod> visited) {
//		Set<AccessGraph> out = new HashSet<>();
//		for (SootMethod m : icfg.getCalleesOfCallAt(call)) {
//			if (visited.contains(m))
//				continue;
//			visited.add(m);
//			if (!m.hasActiveBody())
//				continue;
//			for (Unit u : m.getActiveBody().getUnits()) {
//				if (!(u instanceof AssignStmt))
//					continue;
//				AssignStmt as = (AssignStmt) u;
//
//				if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof NewExpr) {
//					NewExpr expr = ((NewExpr) as.getRightOp());
//					if (allocatesObjectOfInterest(expr)) {
//						Local local = (Local) as.getLeftOp();
//						AccessGraph accessGraph = new AccessGraph(local);
//						out.add(accessGraph.deriveWithAllocationSite(as, expr.getType(), true));
//					}
//				}
//
//			}
//			for (Unit u : icfg.getCallsFromWithin(m))
//				out.addAll(transitivelyReachableAllocationSite(u, visited));
//		}
//		return out;
//	}

	private List<Node<Statement, Value>> parseQuery() {
		LinkedList<Node<Statement,Value>> queries = new LinkedList<>();
		parseQuery(sootTestMethod, queries, new HashSet<SootMethod>());
		if (queries.size() == 0)
			throw new RuntimeException("No call to method queryFor was found within " + sootTestMethod.getName());
		return queries;
	}

	private void parseQuery(SootMethod m, LinkedList<Node<Statement,Value>> queries, Set<SootMethod> visited) {
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
			if(u instanceof AssignStmt){
				AssignStmt as = (AssignStmt) u;
				if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof NewExpr) {
					NewExpr expr = ((NewExpr) as.getRightOp());
					if (allocatesObjectOfInterest(expr)) {
						Local local = (Local) as.getLeftOp();
						for(Unit succ : icfg.getSuccsOf(as))
							queries.add(new Node<Statement,Value>(new Statement((Stmt)succ), local));
					}
				}
			}
//			Stmt stmt = (Stmt) u;
//			if (!(stmt.containsInvokeExpr()))
//				continue;
//			InvokeExpr invokeExpr = stmt.getInvokeExpr();
//			if (!invokeExpr.getMethod().getName().equals("queryFor"))
//				continue;
//			Value param = invokeExpr.getArg(0);
//			if (!(param instanceof Local))
//				continue;
//			Local queryVar = (Local) param;
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
