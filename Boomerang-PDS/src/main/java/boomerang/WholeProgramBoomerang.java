package boomerang;

import java.util.List;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.ReachableMethodListener;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import wpds.impl.Weight;

public abstract class WholeProgramBoomerang<W extends Weight> extends Boomerang<W>{
	private int reachableMethodCount;
	private int allocationSites;
	public void wholeProgramAnalysis(){
		List<SootMethod> reachableMethods = Scene.v().getEntryPoints();
		long before = System.currentTimeMillis();
		for(SootMethod m : reachableMethods){
			addReachable(m);
		}
		
		registerReachableMethodListener(new ReachableMethodListener<W>() {
			@Override
			public void reachable(SootMethod m) {
				analyzeMethod(m);
				reachableMethodCount++;
			}
		});
		long after = System.currentTimeMillis();
		System.out.println("Analysis Time (in ms): \t" + (after-before));
		System.out.println("Analyzed methods:\t" + reachableMethodCount);
		System.out.println("Total solvers:\t" + this.getSolvers().size());
		System.out.println("Allocation Sites:\t" + allocationSites);
	}
	

	public void analyzeMethod(SootMethod method) {
		if(!method.hasActiveBody())
			return;
		for(Unit u : method.getActiveBody().getUnits()){
			if(u instanceof AssignStmt){
				AssignStmt assignStmt = (AssignStmt) u;
				if(isAllocationVal(assignStmt.getRightOp())){
					ForwardQuery q = new ForwardQuery(new Statement((Stmt) u, method), new Val(assignStmt.getLeftOp(),method));
					solve(q);
					allocationSites++;
				}
			}
		}
	}
	
	@Override
	protected void backwardSolve(BackwardQuery query) {
	}
}
