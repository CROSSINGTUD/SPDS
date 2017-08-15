package boomerang;

import boomerang.jimple.Statement;
import soot.Value;

public class BackwardQuery extends Query {
	public BackwardQuery(Statement stmt, Value variable) {
		super(stmt, variable);
	}
	
	@Override
	public String toString() {
		return "BackwardQuery: "+ super.toString();
	}
}
