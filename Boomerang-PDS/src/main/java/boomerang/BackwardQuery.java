package boomerang;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.Value;

public class BackwardQuery extends Query {
	public BackwardQuery(Statement stmt, Val variable) {
		super(stmt, variable);
	}
	
	@Override
	public String toString() {
		return "BackwardQuery: "+ super.toString();
	}
}
