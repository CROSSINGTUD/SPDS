package boomerang.jimple;

import soot.SootMethod;
import soot.Value;
import soot.jimple.NewExpr;

public class AllocVal extends Val {

	private Value alloc;

	public AllocVal(Value v, SootMethod m, Value alloc) {
		super(v, m);
		this.alloc = alloc;
	}

	@Override
	public String toString() {
		return super.toString() + " Value: "+ alloc;
	}
	
	public Value allocationValue(){
		return alloc;
	}

	@Override
	public boolean isNewExpr() {
		return alloc instanceof NewExpr;
	}
}
