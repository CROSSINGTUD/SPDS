package boomerang;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;

public class ForwardQuery extends Query{

	public ForwardQuery(Statement stmt, Val variable) {
		super(stmt, variable);
	}
	@Override
	public String toString() {
		return "ForwardQuery: "+ super.toString();
	}
}
