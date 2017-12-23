package boomerang;

import com.google.common.base.Optional;

import boomerang.jimple.AllocVal;
import boomerang.jimple.Val;
import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class IntAndStringBoomerangOptions extends DefaultBoomerangOptions {

	public boolean isAllocationVal(Value val) {
		if(val instanceof IntConstant){
			return true;
		}
		return super.isAllocationVal(val);
	}

	@Override
	protected boolean isArrayAllocationVal(Value val) {
		return (val instanceof NewArrayExpr || val instanceof NewMultiArrayExpr);
	}

	private Value getArrayLength(Value op) {
		if(op instanceof NewArrayExpr){
			return ((NewArrayExpr) op).getSize();
		} else if (op instanceof NewMultiArrayExpr){
			return ((NewMultiArrayExpr) op).getSize(0);
		}
		throw new RuntimeException("Illegal State");
	}
	@Override
	public Optional<AllocVal> getAllocationVal(SootMethod m, Stmt stmt, Val fact, BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
		if (!(stmt instanceof AssignStmt)) {
			return Optional.absent();
		}
		AssignStmt as = (AssignStmt) stmt;
		if(fact.value() instanceof LengthExpr){
			if (as.getLeftOp().equals(((LengthExpr) fact.value()).getOp())) {
				if(isArrayAllocationVal(as.getRightOp())){
					return Optional.of(new AllocVal(fact.value(), m,getArrayLength(as.getRightOp())));
				}
			}
		}
		if (!as.getLeftOp().equals(fact.value())) {
			return Optional.absent();
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
		return super.getAllocationVal(m, stmt, fact, icfg);
	}


	@Override
	public boolean trackStrings() {
		return true;
	}
}
