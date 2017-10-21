package boomerang;

import boomerang.jimple.Statement;

public class UnbalancedForwardQuery extends ForwardQuery {

	private ForwardQuery original;

	public UnbalancedForwardQuery(Statement stmt, ForwardQuery delegate) {
		super(stmt, delegate.asNode().fact());
		this.original = delegate;
	}
	
	public ForwardQuery sourceQuery(){
		return original;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((original == null) ? 0 : original.hashCode());
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
		UnbalancedForwardQuery other = (UnbalancedForwardQuery) obj;
		if (original == null) {
			if (other.original != null)
				return false;
		} else if (!original.equals(other.original))
			return false;
		return true;
	}

	
	@Override
	public String toString() {
		return super.toString() + " allocated at " + original.asNode().stmt();
	}
}
