package boomerang.jimple;

import soot.SootMethod;
import soot.Value;

public class Val {
	private final SootMethod m;
	private final Value v;

	public Val(Value v, SootMethod m){
		this.v = v;
		this.m = m;
	}

	public Value value(){
		return v;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Val other = (Val) obj;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return v.toString() ;//+ " (" + m.getDeclaringClass().getShortName() +"." + m.getName() +")";
	}
}
