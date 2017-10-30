package boomerang;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;

public class UnbalancedForwardQuery extends ForwardQuery {


	public UnbalancedForwardQuery(Statement stmt,  Val variable) {
		super(stmt, variable);
	}

	
	@Override
	public String toString() {
		return "Unbalanced"+super.toString();
	}
}
