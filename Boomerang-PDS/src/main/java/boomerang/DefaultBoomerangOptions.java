package boomerang;

import soot.RefType;
import soot.Type;
import soot.Value;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;

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
		return val instanceof NewExpr;
	}

	private boolean isStringAllocationType(Type type) {
		return type.toString().equals("java.lang.String") || type.toString().equals("java.lang.StringBuilder")
				|| type.toString().equals("java.lang.StringBuffer");
	}

	private boolean isArrayAllocationVal(Value val) {
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

}
