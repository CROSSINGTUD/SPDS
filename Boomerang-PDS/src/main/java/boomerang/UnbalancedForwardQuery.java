package boomerang;

import boomerang.jimple.Statement;
import boomerang.jimple.StatementWithAlloc;
import boomerang.jimple.Val;

public class UnbalancedForwardQuery extends ForwardQuery {


	public UnbalancedForwardQuery(Statement stmt,  Val variable) {
		super(stmt, variable);
	}

	public Query unwrap(){
		return new ForwardQuery(((StatementWithAlloc) stmt()).getAlloc(), var());
	}
	
	@Override
	public String toString() {
		return "Unbalanced"+super.toString();
	}
}
