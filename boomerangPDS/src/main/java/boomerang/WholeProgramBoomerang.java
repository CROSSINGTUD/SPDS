package boomerang;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.seedfactory.SeedFactory;
import boomerang.solver.ReachableMethodListener;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import wpds.impl.Weight;
import wpds.impl.Weight.NoWeight;

public abstract class WholeProgramBoomerang<W extends Weight> extends WeightedBoomerang<W>{
	private int reachableMethodCount;
	private int allocationSites;
	private SeedFactory<W> seedFactory;

	public WholeProgramBoomerang(BoomerangOptions opts){
		super(opts);
	}
	
	public WholeProgramBoomerang(){
		this(new DefaultBoomerangOptions());
	}

	@Override
	public SeedFactory<W> getSeedFactory() {
		return seedFactory;
	}
	
	public void wholeProgramAnalysis(){
		long before = System.currentTimeMillis();
		seedFactory = new SeedFactory<W>() {

			@Override
			protected Collection<? extends Query> generate(SootMethod method, Stmt u,
					Collection<SootMethod> calledMethods) {
				if(u instanceof AssignStmt){
					AssignStmt assignStmt = (AssignStmt) u;
					if(options.isAllocationVal(assignStmt.getRightOp())){
						return Collections.singleton(new ForwardQuery(new Statement((Stmt) u, method), new AllocVal(assignStmt.getLeftOp(),method,assignStmt.getRightOp())));
					}
				}
				return Collections.emptySet();
			}

			@Override
			public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
				return WholeProgramBoomerang.this.icfg();
			}
			@Override
			protected boolean analyseClassInitializers() {
				return true;
			}
		};
		for(Query s :seedFactory.computeSeeds()){
			solve(s);
		}
		
		long after = System.currentTimeMillis();
		System.out.println("Analysis Time (in ms):\t" + (after-before));
		System.out.println("Analyzed methods:\t" + reachableMethodCount);
		System.out.println("Total solvers:\t" + this.getSolvers().size());
		System.out.println("Allocation Sites:\t" + allocationSites);
		System.out.println(options.statsFactory());
	}
	

	
	@Override
	protected void backwardSolve(BackwardQuery query) {
	}
}
