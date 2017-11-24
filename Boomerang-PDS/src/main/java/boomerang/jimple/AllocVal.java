package boomerang.jimple;

import soot.SootMethod;
import soot.Value;

public class AllocVal extends Val {

	private Value alloc;

	public AllocVal(Value v, SootMethod m, Value alloc) {
		super(v, m);
		this.alloc = alloc;
	}

	@Override
	public Val asNoAlloc(){
		return new Val(this.value(),this.m());
	}

	@Override
	public String toString() {
		return super.toString() + " Value: "+ alloc;
	}

}
