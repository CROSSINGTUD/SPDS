package boomerang;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.nodes.Node;

public class UnbalancedForwardQuery extends ForwardQuery {

	private Query original;

	public UnbalancedForwardQuery(Statement stmt, Query delegate) {
		super(stmt, delegate.asNode().fact());
		this.original = delegate;
	}
	
	public Query sourceQuery(){
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
