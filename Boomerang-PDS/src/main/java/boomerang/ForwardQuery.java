package boomerang;

import boomerang.jimple.Statement;
import soot.Value;
import sync.pds.solver.nodes.Node;

public class ForwardQuery extends Query{

	public ForwardQuery(Statement stmt, Value variable) {
		super(stmt, variable);
	}
}
