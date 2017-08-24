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
}
