package test.core.selfrunning;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.Timeout;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WholeProgramBoomerang;
import boomerang.debugger.Debugger;
import boomerang.debugger.IDEVizDebugger;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
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
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sync.pds.solver.EmptyStackWitnessListener;
import sync.pds.solver.nodes.Node;

public class AbstractBoomerangTest extends AbstractTestingFramework {

    @Rule
    public Timeout timeout = new Timeout(10000000);
	private JimpleBasedInterproceduralCFG icfg;			
	private Collection<? extends Query> allocationSites;
	protected Collection<? extends Query> queryForCallSites;
	protected Collection<Error> unsoundErrors = Sets.newHashSet();
	protected Collection<Error> imprecisionErrors = Sets.newHashSet();


	private enum AnalysisMode{
		WholeProgram, DemandDrivenForward, DemandDrivenBackward;
	}
	
	protected AnalysisMode[] getAnalyses(){
		return new AnalysisMode[]{
			AnalysisMode.WholeProgram,
//			AnalysisMode.DemandDrivenForward, 
//			AnalysisMode.DemandDrivenBackward
		};
	}
	
	protected SceneTransformer createAnalysisTransformer() {
		return new SceneTransformer() {

			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				icfg = new JimpleBasedInterproceduralCFG(true);
				allocationSites = extractQuery(new AllocationSiteOf());
				queryForCallSites = extractQuery(
						new FirstArgumentOf("queryFor"));
				
				for(AnalysisMode analysis : getAnalyses()){
					switch(analysis){
						case WholeProgram: 
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
				if(!unsoundErrors.isEmpty()){
					throw new RuntimeException(Joiner.on("\n").join(unsoundErrors));
				}
				if(!imprecisionErrors.isEmpty()){
					throw new AssertionError(Joiner.on("\n").join(imprecisionErrors));
				}
			}
		};
	}

	
	private void runDemandDrivenBackward() {
		//Run backward analysis
		if(queryForCallSites.size() > 1)
			throw new RuntimeException("Found more than one backward query to execute!");
		Set<Node<Statement, Val>> backwardResults = runQuery(queryForCallSites);
		compareQuery(allocationSites, backwardResults, AnalysisMode.DemandDrivenBackward);
	}

	private void runDemandDrivenForward() {
		//Run forward analysis
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
						return Optional.<Query>of(new ForwardQuery(new Statement(unit, icfg.getMethodOf(unit)), new Val(local,icfg.getMethodOf(unit))));
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
			return Optional.<Query>of(new BackwardQuery(new Statement(unit, icfg.getMethodOf(unit)), new Val(param,icfg.getMethodOf(unit))));
		}
	}

	private void compareQuery(Collection<? extends Query> expectedResults,
			Collection<? extends Node<Statement, Val>> results, AnalysisMode analysis) {
		System.out.println("Boomerang Allocations Sites: " + results);
		System.out.println("Boomerang Results: " + results);
		System.out.println("Expected Results: " + expectedResults);
		Collection<Node<Statement, Val>> falseNegativeAllocationSites = new HashSet<>();
		for(Query res : expectedResults){
			if(!results.contains(res.asNode()))
				falseNegativeAllocationSites.add(res.asNode());
		}
		Collection<? extends Node<Statement, Val>> falsePositiveAllocationSites = new HashSet<>(results);
		for(Query res : expectedResults){
			falsePositiveAllocationSites.remove(res.asNode());
		}

		String answer = (falseNegativeAllocationSites.isEmpty() ? "" : "\nFN:" + falseNegativeAllocationSites)
				+ (falsePositiveAllocationSites.isEmpty() ? "" : "\nFP:" + falsePositiveAllocationSites + "\n");
		if (!falseNegativeAllocationSites.isEmpty()) {
			unsoundErrors.add(new Error(analysis + " Unsound results for:" + answer));
		}
		if(!falsePositiveAllocationSites.isEmpty())
			imprecisionErrors.add(new Error(analysis + " Imprecise results for:" + answer));
	}

	private Set<Node<Statement, Val>> runQuery(Collection<? extends Query> queries) {
		final Set<Node<Statement, Val>> results = Sets.newHashSet();
		for (Query query : queries) {
			Boomerang solver = new Boomerang() {
				@Override
				public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
					return icfg;
				}
				
				@Override
				public Debugger createDebugger() {
					return new IDEVizDebugger(ideVizFile,icfg);
				}
			};
			if(query instanceof BackwardQuery){
				solver.addBackwardQuery((BackwardQuery)query,new EmptyStackWitnessListener<Statement, Val>() {
					@Override
					public void witnessFound(Node<Statement, Val> allocation) {
						results.add(allocation);	
					}
				});
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
		WholeProgramBoomerang solver = new WholeProgramBoomerang() {
			@Override
			public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
				return icfg;
			}
			
			@Override
			public Debugger createDebugger() {
				return new IDEVizDebugger(ideVizFile,icfg);
			}
		};
		solver.wholeProgramAnalysis();
		DefaultValueMap<Query, AbstractBoomerangSolver> solvers = solver.getSolvers();
		for(final Query q : solvers.keySet()){
			if(!(q instanceof ForwardQuery))
				throw new RuntimeException("Unexpected solver found, whole program analysis should only trigger forward queries");
			for(Query queryForCallSite : queryForCallSites){
			solvers.get(q).synchedEmptyStackReachable(queryForCallSite.asNode(), new EmptyStackWitnessListener<Statement, Val>() {
				@Override
				public void witnessFound(Node<Statement, Val> targetFact) {
					results.add(q.asNode());
				}
				});
			}
			for(Node<Statement, Val> s : solvers.get(q).getReachedStates()){
				if(s.stmt().getMethod().toString().contains("unreachable") && !q.toString().contains("dummyClass.main")){
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
