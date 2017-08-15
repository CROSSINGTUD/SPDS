package boomerang;

import boomerang.jimple.Statement;
import soot.Value;
import sync.pds.solver.nodes.Node;

public abstract class Query{

	private final Node<Statement, Value> delegate;

	public Query(Statement stmt, Value variable) {
		delegate = new Node<Statement,Value>(stmt,variable);
	}

	public Node<Statement,Value> asNode(){
		return delegate;
	}
	@Override
	public String toString() {
		return delegate.toString();
	}
}
