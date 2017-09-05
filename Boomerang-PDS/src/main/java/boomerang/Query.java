package boomerang;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.nodes.Node;

public abstract class Query{

	private final Node<Statement, Val> delegate;

	public Query(Statement stmt, Val variable) {
		delegate = new Node<Statement,Val>(stmt,variable);
	}

	public Node<Statement,Val> asNode(){
		return delegate;
	}
	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
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
		Query other = (Query) obj;
		if (delegate == null) {
			if (other.delegate != null)
				return false;
		} else if (!delegate.equals(other.delegate))
			return false;
		return true;
	}
}
