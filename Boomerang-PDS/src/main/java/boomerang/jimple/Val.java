package boomerang.jimple;

import soot.SootMethod;
import soot.Value;

public class Val {
	private final SootMethod m;
	private final Value v;
	private final String rep; 

	private static Val staticInstance;
	
	public Val(Value v, SootMethod m){
		this.v = v;
		this.m = m;
		this.rep = null;
	}
	
	private Val(String rep){
		this.rep = rep;
		this.m = null;
		this.v = null;
	}

	public Value value(){
		return v;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
//		result = prime * result + ((m == null) ? 0 : m.hashCode());
		result = prime * result + ((rep == null) ? 0 : rep.hashCode());
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
//		if (m == null) {
//			if (other.m != null)
//				return false;
//		} else if (!m.equals(other.m))
//			return false;
		if (rep == null) {
			if (other.rep != null)
				return false;
		} else if (!rep.equals(other.rep))
			return false;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if(rep != null)
			return rep;
		return v.toString();//+ " (" + m.getDeclaringClass().getShortName() +"." + m.getName() +")";
	}

	public static Val statics() {
		if(staticInstance == null)
			staticInstance = new Val("STATIC");
		return staticInstance;
	}

}
