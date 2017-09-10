package boomerang;

import java.util.List;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.ReachableMethodListener;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;

public abstract class WholeProgramBoomerang extends Boomerang{
	public void wholeProgramAnalysis(){
		List<SootMethod> reachableMethods = Scene.v().getEntryPoints();
		for(SootMethod m : reachableMethods){
			analyzeMethod(m);
		}
		registerReachableMethodListener(new ReachableMethodListener() {
			
			@Override
			public void reachable(AbstractBoomerangSolver solver, SootMethod m) {
				analyzeMethod(m);
			}
		});
	}
	
	public void analyzeMethod(SootMethod method) {
		for(Unit u : method.getActiveBody().getUnits()){
			if(u instanceof AssignStmt){
				AssignStmt assignStmt = (AssignStmt) u;
				if(assignStmt.getRightOp() instanceof NewExpr){
					
					NewExpr newExpr = (NewExpr) assignStmt.getRightOp();

					solve(new ForwardQuery(new Statement((Stmt) u, method), new Val(assignStmt.getLeftOp(),method)));
				}
			}
		}
	}
	
	@Override
	protected void backwardSolve(BackwardQuery query) {
	}
}
