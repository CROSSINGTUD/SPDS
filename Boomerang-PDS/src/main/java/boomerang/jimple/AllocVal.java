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
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((alloc == null) ? 0 : alloc.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AllocVal other = (AllocVal) obj;
		if (alloc == null) {
			if (other.alloc != null)
				return false;
		} else if (!alloc.equals(other.alloc))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return super.toString() + " Value: "+ alloc;
	}

}
