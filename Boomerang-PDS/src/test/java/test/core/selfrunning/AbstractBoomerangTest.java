package test.core.selfrunning;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Predicate;
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
import soot.jimple.InvokeExpr;
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
				Collection<Node<Statement, Value>> q = extractQuery(new ValueOfInterestInUnit(){
					@Override
					public Optional<Value> test(Stmt unit) {
						if(unit instanceof AssignStmt){
							AssignStmt as = (AssignStmt) unit;
							if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof NewExpr) {
								NewExpr expr = ((NewExpr) as.getRightOp());
								if (allocatesObjectOfInterest(expr)) {
									Local local = (Local) as.getLeftOp();
									return Optional.<Value>of(local);
								}
							}
						}
						return Optional.empty();
					}

					@Override
					public boolean atSuccessor() {
						return true;
					}} );

				Collection<Node<Statement,Value>> expectedResults = extractQuery(new ValueOfInterestInUnit() {
					@Override
					public Optional<? extends Value> test(Stmt unit) {
						Stmt stmt = (Stmt) unit;
						if (!(stmt.containsInvokeExpr()))
							return Optional.empty();
						InvokeExpr invokeExpr = stmt.getInvokeExpr();
						if (!invokeExpr.getMethod().getName().matches("(queryFor|reachable)"))
							return Optional.empty();
						Value param = invokeExpr.getArg(0);
						if (!(param instanceof Local))
							return Optional.empty();
						return Optional.<Value>of(param);
					}

					@Override
					public boolean atSuccessor() {
						return false;
					}
				});
				Collection<Node<Statement,Value>> unreachableNodes = extractQuery(new ValueOfInterestInUnit() {
					@Override
					public Optional<? extends Value> test(Stmt unit) {
						Stmt stmt = (Stmt) unit;
						if (!(stmt.containsInvokeExpr()))
							return Optional.empty();
						InvokeExpr invokeExpr = stmt.getInvokeExpr();
						if (!invokeExpr.getMethod().getName().equals("unreachable"))
							return Optional.empty();
						Value param = invokeExpr.getArg(0);
						if (!(param instanceof Local))
							return Optional.empty();
						return Optional.<Value>of(param);
					}

					@Override
					public boolean atSuccessor() {
						return false;
					}
				});
				Collection<Node<Statement,Value>> results = runQuery(q);
				compareQuery(q, expectedResults, unreachableNodes, results);
			}
		};
	}

	private void compareQuery(Collection<Node<Statement, Value>> q, Collection<Node<Statement,Value>> expectedResults,Collection<Node<Statement,Value>> unreachableNodes, Collection<Node<Statement,Value>> results) {
		System.out.println("Boomerang Allocations Sites: " + results);
		System.out.println("Boomerang Results: " + results);
		System.out.println("Expected Results: " + expectedResults);
		Collection<Node<Statement,Value>> falseNegativeAllocationSites = new HashSet<>(expectedResults);
		falseNegativeAllocationSites.removeAll(results);
		Collection<Node<Statement,Value>> falsePositiveAllocationSites = new HashSet<>(results);
		falsePositiveAllocationSites.removeAll(expectedResults);
		for(Node<Statement, Value> n : unreachableNodes){
			if(results.contains(n)){
				throw new RuntimeException("Unreachable node discovered "+ n);
			}
		}
		
		String answer = (falseNegativeAllocationSites.isEmpty() ? "" : "\nFN:" + falseNegativeAllocationSites)
				+ (falsePositiveAllocationSites.isEmpty() ? "" : "\nFP:" + falsePositiveAllocationSites + "\n");
		if (!falseNegativeAllocationSites.isEmpty()) {
			throw new RuntimeException("Unsound results for:" + answer);
		}
	}

	private Set<Node<Statement, Value>> runQuery(Collection<Node<Statement, Value>> queries) {
		Set<Node<Statement,Value>> results = Sets.newHashSet();
		for(Node<Statement, Value> query : queries){
			BoomerangSolver solver = new BoomerangSolver(icfg);
			solver.solve(new Node<Statement, Value>(query.stmt(),query.fact()));
			results.addAll(solver.getReachedStates());
		}
		return results;
	}

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

	private Collection<Node<Statement, Value>> extractQuery(ValueOfInterestInUnit predicate) {
		Set<Node<Statement,Value>> queries = Sets.newHashSet();
		extractQuery(sootTestMethod, predicate, queries, new HashSet<SootMethod>());
		return queries;
	}

	private void extractQuery(SootMethod m, ValueOfInterestInUnit predicate, Collection<Node<Statement,Value>> queries, Set<SootMethod> visited) {
		if (!m.hasActiveBody() || visited.contains(m))
			return;
		visited.add(m);
		Body activeBody = m.getActiveBody();
		for (Unit callSite : icfg.getCallsFromWithin(m)) {
			for (SootMethod callee : icfg.getCalleesOfCallAt(callSite))
				extractQuery(callee, predicate, queries, visited);
		}
		for (Unit u : activeBody.getUnits()) {
			if (!(u instanceof Stmt))
				continue;
			Optional<? extends Value> optOfVal = predicate.test((Stmt) u);
			if(optOfVal.isPresent()){
				if(!predicate.atSuccessor()){
					queries.add(new Node<Statement,Value>(new Statement((Stmt)u,m), optOfVal.get()));
				} else{
					for(Unit succ : icfg.getSuccsOf(u)){
						queries.add(new Node<Statement,Value>(new Statement((Stmt)succ,m), optOfVal.get()));
					}
				}
			}
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
	 * A call to this method flags the object as at the call statement as not reachable by the analysis.
	 * @param variable
	 */
	protected void unreachable(Object variable) {

	}
	
	/**
	 * A call to this method flags the object as at the call statement as reachable by the analysis.
	 * @param variable
	 */
	protected void reachable(Object variable) {

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

	private interface ValueOfInterestInUnit{
		Optional<? extends Value> test(Stmt unit);
		boolean atSuccessor();
	}
}
