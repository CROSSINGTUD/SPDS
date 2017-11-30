package boomerang;

import com.google.common.base.Optional;

import boomerang.jimple.AllocVal;
import boomerang.jimple.Val;
import java_cup.reduce_action;
import soot.RefType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class DefaultBoomerangOptions implements BoomerangOptions {
	
	private boolean NULL_ALLOCATIONS = false;
	private boolean TRACK_STRINGS = true;

	public boolean isAllocationVal(Value val) {
		if (!TRACK_STRINGS && isStringAllocationType(val.getType())) {
			return false;
		}
		if(NULL_ALLOCATIONS && val instanceof NullConstant){
			return true;
		}
		if(arrayFlows() && isArrayAllocationVal(val)){
			return true;
		}
		if(TRACK_STRINGS && val instanceof StringConstant){
			return true;
		}
		return val instanceof NewExpr;
	}

	private boolean isStringAllocationType(Type type) {
		return type.toString().equals("java.lang.String") || type.toString().equals("java.lang.StringBuilder")
				|| type.toString().equals("java.lang.StringBuffer");
	}

	protected boolean isArrayAllocationVal(Value val) {
		if(val instanceof NewArrayExpr){
			NewArrayExpr expr = (NewArrayExpr) val;
			return expr.getBaseType() instanceof RefType;
		} else if(val instanceof NewMultiArrayExpr){
			NewMultiArrayExpr expr = (NewMultiArrayExpr) val;
			return expr.getBaseType().getArrayElementType() instanceof RefType;
		}
		return false;
	}
	
	@Override
	public boolean staticFlows() {
		return true;
	}

	@Override
	public boolean arrayFlows() {
		return true;
	}

	@Override
	public boolean fastForwardFlows() {
		return true;
	}

	@Override
	public boolean typeCheck() {
		return true;
	}

	@Override
	public boolean onTheFlyCallGraph() {
		return false;
	}

	@Override
	public boolean throwFlows() {
		return false;
	}

	@Override
	public boolean callSummaries() {
		return false;
	}

	@Override
	public boolean fieldSummaries() {
		return false;
	}

	@Override
	public Optional<AllocVal> getAllocationVal(SootMethod m, Stmt stmt, Val fact, BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
		if (!(stmt instanceof AssignStmt)) {
			return Optional.absent();
		}
		AssignStmt as = (AssignStmt) stmt;
		if (!as.getLeftOp().equals(fact.value())) {
			return Optional.absent();
		}
		if(isAllocationVal(as.getRightOp())) {
			return Optional.of(new AllocVal(as.getLeftOp(), m,as.getRightOp()));
		}
		if(as.containsInvokeExpr()){
			for(SootMethod callee : icfg.getCalleesOfCallAt(as)){
				for(Unit u : icfg.getEndPointsOf(callee)){
					if(u instanceof ReturnStmt && isAllocationVal(((ReturnStmt) u).getOp())){
						return Optional.of(new AllocVal(as.getLeftOp(), m,((ReturnStmt) u).getOp()));
					}
				}
			}
		}
		return Optional.absent();
	}

}
