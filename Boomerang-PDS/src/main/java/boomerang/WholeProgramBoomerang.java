package boomerang;

import java.util.List;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.ReachableMethodListener;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import wpds.impl.Weight;

public abstract class WholeProgramBoomerang<W extends Weight> extends Boomerang<W>{

	public void wholeProgramAnalysis(){
		List<SootMethod> reachableMethods = Scene.v().getEntryPoints();
		
		for(SootMethod m : reachableMethods){
			analyzeMethod(m);
		}
		registerReachableMethodListener(new ReachableMethodListener<W>() {
			@Override
			public void reachable(SootMethod m) {
				analyzeMethod(m);
			}
		});
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
				}
			}
		}
	}
	
	@Override
	protected void backwardSolve(BackwardQuery query) {
	}
}
