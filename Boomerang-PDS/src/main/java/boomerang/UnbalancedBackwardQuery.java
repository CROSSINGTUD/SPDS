package boomerang;

import boomerang.jimple.Statement;

public class UnbalancedBackwardQuery extends BackwardQuery {

	private BackwardQuery original;

	public UnbalancedBackwardQuery(Statement stmt, BackwardQuery delegate) {
		super(stmt, delegate.asNode().fact());
		this.original = delegate;
	}
	
	public BackwardQuery sourceQuery(){
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
		UnbalancedBackwardQuery other = (UnbalancedBackwardQuery) obj;
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
