package test.core.selfrunning;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Lists;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import heros.InterproceduralCFG;
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
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.WitnessNode.WitnessListener;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;

public class AbstractBoomerangTest extends AbstractTestingFramework {
	private JimpleBasedInterproceduralCFG icfg;

	protected SceneTransformer createAnalysisTransformer() {
		return new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				icfg = new JimpleBasedInterproceduralCFG(true);
				Collection<? extends Query> queryForCallSites = extractQuery(
						new FirstArgumentOf("queryFor"));
				Collection<? extends Query> allocationSites = extractQuery(new AllocationSiteOf());

				//Run forward analysis
				Collection<? extends Query> expectedResults = extractQuery(
						new FirstArgumentOf("reachable"));
				Collection<? extends Query> unreachableNodes = extractQuery(
						new FirstArgumentOf("unreachable"));
				Collection<Node<Statement, Value>> results = runQuery(allocationSites);
				compareQuery( expectedResults, unreachableNodes, results, "Forward");
				if(!queryForCallSites.isEmpty()){
					//Run backward analysis
					if(queryForCallSites.size() > 1)
						throw new RuntimeException("Found more than one backward query to execute!");
//					Collection<? extends Query> expectedResults = extractQuery(
//							new FirstArgumentOf("reachable"));
					unreachableNodes = extractQuery(
							new FirstArgumentOf("unreachable"));
					results = runQuery(queryForCallSites);
					compareQuery(allocationSites, unreachableNodes, results, "Backward");
				}
				
			}
		};
	}

	private class AllocationSiteOf implements ValueOfInterestInUnit {
		public Optional<? extends Query> test(Stmt unit) {
			if (unit instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) unit;
				if (as.getLeftOp() instanceof Local && as.getRightOp() instanceof NewExpr) {
					NewExpr expr = ((NewExpr) as.getRightOp());
					if (allocatesObjectOfInterest(expr)) {
						Local local = (Local) as.getLeftOp();
						return Optional.<Query>of(new ForwardQuery(new Statement(unit, icfg.getMethodOf(unit)), local));
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
		public Optional<? extends Query> test(Stmt unit) {
			Stmt stmt = (Stmt) unit;
			if (!(stmt.containsInvokeExpr()))
				return Optional.empty();
			InvokeExpr invokeExpr = stmt.getInvokeExpr();
			if (!invokeExpr.getMethod().getName().matches(methodNameMatcher))
				return Optional.empty();
			Value param = invokeExpr.getArg(0);
			if (!(param instanceof Local))
				return Optional.empty();
			return Optional.<Query>of(new BackwardQuery(new Statement(unit, icfg.getMethodOf(unit)), param));
		}
	}

	private void compareQuery(Collection<? extends Query> expectedResults,
			Collection<? extends Query> unreachableNodes,
			Collection<? extends Node<Statement, Value>> results, String analysis) {
		System.out.println("Boomerang Allocations Sites: " + results);
		System.out.println("Boomerang Results: " + results);
		System.out.println("Expected Results: " + expectedResults);
		Collection<Query> falseNegativeAllocationSites = new HashSet<>();
		for(Query res : expectedResults){
			if(!results.contains(res.asNode()))
				falseNegativeAllocationSites.add(res);
		}
		Collection<? extends Node<Statement, Value>> falsePositiveAllocationSites = new HashSet<>(results);
		for(Query res : expectedResults){
			falsePositiveAllocationSites.remove(res.asNode());
		}
		for (Query n : unreachableNodes) {
			if (results.contains(n.asNode())) {
				throw new RuntimeException("Unreachable node discovered " + n);
			}
		}

		String answer = (falseNegativeAllocationSites.isEmpty() ? "" : "\nFN:" + falseNegativeAllocationSites)
				+ (falsePositiveAllocationSites.isEmpty() ? "" : "\nFP:" + falsePositiveAllocationSites + "\n");
		if (!falseNegativeAllocationSites.isEmpty()) {
			throw new RuntimeException(analysis + " Unsound results for:" + answer);
		}
	}

	private Set<Node<Statement, Value>> runQuery(Collection<? extends Query> queries) {
		final Set<Node<Statement, Value>> results = Sets.newHashSet();
		for (Query query : queries) {
			Boomerang solver = new Boomerang() {
				@Override
				public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
					return icfg;
				}
			};
			System.out.println(query.asNode().stmt().getMethod().getActiveBody());
			if(query instanceof BackwardQuery){
				WitnessListener<Statement, Value, Field> a;
				solver.addBackwardQuery((BackwardQuery)query, new WitnessListener<Statement, Value, Field>() {

					@Override
					public void onAddCallWitnessTransition(Transition<Statement, INode<Value>> t) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void onAddFieldWitnessTransition(Transition<Field, INode<Node<Statement, Value>>> t) {
						if(!(t.getTarget() instanceof GeneratedState))
							results.add(t.getTarget().fact());
					}
				});
			}else{
				solver.solve(query);
				results.addAll(solver.getForwardReachableStates());
			}
		}
		return results;
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
		extractQuery(sootTestMethod, predicate, queries, new HashSet<SootMethod>());
		return queries;
	}

	private void extractQuery(SootMethod m, ValueOfInterestInUnit predicate, Collection<Query> queries,
			Set<SootMethod> visited) {
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

	/**
	 * A call to this method flags the object as at the call statement as not
	 * reachable by the analysis.
	 * 
	 * @param variable
	 */
	protected void unreachable(Object variable) {

	}

	/**
	 * A call to this method flags the object as at the call statement as
	 * reachable by the analysis.
	 * 
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

	private interface ValueOfInterestInUnit {
		Optional<? extends Query> test(Stmt unit);
	}
}
