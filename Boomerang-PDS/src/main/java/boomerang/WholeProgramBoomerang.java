package boomerang;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.util.queue.QueueReader;

public abstract class WholeProgramBoomerang extends Boomerang{
	public void wholeProgramAnalysis(){
		QueueReader<MethodOrMethodContext> reachableMethods = Scene.v().getReachableMethods().listener();
		while(reachableMethods.hasNext()){
			MethodOrMethodContext next = reachableMethods.next();
			SootMethod method = next.method();
			if(!method.hasActiveBody())
				continue;
			for(Unit u : method.getActiveBody().getUnits()){
				if(u instanceof AssignStmt){
					AssignStmt assignStmt = (AssignStmt) u;
					if(assignStmt.getRightOp() instanceof NewExpr){
						solve(new ForwardQuery(new Statement((Stmt) u, method), new Val(assignStmt.getLeftOp(),method)));
					}
				}
			}
		}
	}
	
	@Override
	protected void backwardSolve(BackwardQuery query) {
	}
}
