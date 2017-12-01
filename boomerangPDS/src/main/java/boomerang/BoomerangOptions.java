package boomerang;

import com.google.common.base.Optional;

import boomerang.jimple.AllocVal;
import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public interface BoomerangOptions {
	
	public boolean staticFlows();
	
	public boolean arrayFlows();
	public boolean fastForwardFlows();
	public boolean typeCheck();
	public boolean onTheFlyCallGraph();
	public boolean throwFlows();
	
	public boolean callSummaries();
	public boolean fieldSummaries();
	
	public int analysisTimeoutMS();

	public boolean isAllocationVal(Value val);

	public Optional<AllocVal> getAllocationVal(SootMethod m, Stmt stmt, Val fact, BiDiInterproceduralCFG<Unit, SootMethod> icfg);
}
