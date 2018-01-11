package boomerang;

import java.util.List;

import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.solver.ReachableMethodListener;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import wpds.impl.Weight;

public abstract class WholeProgramBoomerang<W extends Weight> extends WeightedBoomerang<W>{
	private int reachableMethodCount;
	private int allocationSites;

	public WholeProgramBoomerang(BoomerangOptions opts){
		super(opts);
	}
	
	public WholeProgramBoomerang(){
		this(new DefaultBoomerangOptions());
	}

	public void wholeProgramAnalysis(){
//		System.out.println("Tracking Strings: " + Boomerang.TRACK_STRING);
//		System.out.println("Tracking Arrays: " + Boomerang.TRACK_ARRAYS);
//		System.out.println("Tracking Static: " + Boomerang.TRACK_STATIC);
		List<SootMethod> entryPoints = Scene.v().getEntryPoints();
		long before = System.currentTimeMillis();
		for(SootMethod m : entryPoints){
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
		System.out.println("Analysis Time (in ms):\t" + (after-before));
		System.out.println("Analyzed methods:\t" + reachableMethodCount);
		System.out.println("Total solvers:\t" + this.getSolvers().size());
		System.out.println("Allocation Sites:\t" + allocationSites);
		System.out.println(options.statsFactory());
	}
	

	public void analyzeMethod(SootMethod method) {
		if(!method.hasActiveBody())
			return;
		for(Unit u : method.getActiveBody().getUnits()){
			if(u instanceof AssignStmt){
				AssignStmt assignStmt = (AssignStmt) u;
				if(options.isAllocationVal(assignStmt.getRightOp())){
					ForwardQuery q = new ForwardQuery(new Statement((Stmt) u, method), new AllocVal(assignStmt.getLeftOp(),method,assignStmt.getRightOp()));
					solve(q);
					allocationSites++;
				}
			}
			if(((Stmt) u).containsInvokeExpr()){
				InvokeExpr invokeExpr = ((Stmt) u).getInvokeExpr();
				if(invokeExpr.getMethod().isStatic()){
					addReachable(invokeExpr.getMethod());
				}
			}
		}
	}
	
	@Override
	protected void backwardSolve(BackwardQuery query) {
	}
}
