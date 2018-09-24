package boomerang.jimple;

public class UnbalancedVal extends Val {

	private Statement callStatement;

	public UnbalancedVal(Val val, Statement callStatement) {
		super(val.value(), val.m());
		this.callStatement = callStatement;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((callStatement == null) ? 0 : callStatement.hashCode());
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
		UnbalancedVal other = (UnbalancedVal) obj;
		if (callStatement == null) {
			if (other.callStatement != null)
				return false;
		} else if (!callStatement.equals(other.callStatement))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "Unbalanced Val " + this.value() + " @ " + callStatement; 
	}
}
