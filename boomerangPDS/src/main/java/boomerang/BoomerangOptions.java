package boomerang;

import boomerang.stats.IBoomerangStats;
import com.google.common.base.Optional;

import boomerang.jimple.AllocVal;
import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public interface BoomerangOptions {
	
	boolean staticFlows();
	
	boolean arrayFlows();
	boolean fastForwardFlows();
	boolean typeCheck();
	boolean onTheFlyCallGraph();
	boolean throwFlows();
	
	boolean callSummaries();
	boolean fieldSummaries();
	
	int analysisTimeoutMS();

	boolean isAllocationVal(Value val);
	Optional<AllocVal> getAllocationVal(SootMethod m, Stmt stmt, Val fact, BiDiInterproceduralCFG<Unit, SootMethod> icfg);


	boolean isIgnoredMethod(SootMethod method);
	IBoomerangStats statsFactory();
}
