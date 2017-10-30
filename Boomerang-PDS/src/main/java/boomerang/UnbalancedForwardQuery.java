package boomerang;

import boomerang.jimple.Statement;

public class UnbalancedForwardQuery extends ForwardQuery {

	private Statement unbalancedCallSite;

	public UnbalancedForwardQuery(Statement stmt, ForwardQuery delegate) {
		super(delegate.asNode().stmt(), delegate.asNode().fact());
		this.unbalancedCallSite = stmt;
	}

	
	@Override
	public String toString() {
		return super.toString() + " returns via " + unbalancedCallSite;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((unbalancedCallSite == null) ? 0 : unbalancedCallSite.hashCode());
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
		if (unbalancedCallSite == null) {
			if (other.unbalancedCallSite != null)
				return false;
		} else if (!unbalancedCallSite.equals(other.unbalancedCallSite))
			return false;
		return true;
	}
}
