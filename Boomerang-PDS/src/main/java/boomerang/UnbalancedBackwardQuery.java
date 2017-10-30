package boomerang;

import boomerang.jimple.Statement;

public class UnbalancedBackwardQuery extends BackwardQuery {


	private final Statement unbalancedReturnSite;


	public UnbalancedBackwardQuery(Statement stmt, BackwardQuery delegate) {
		super(delegate.asNode().stmt(), delegate.asNode().fact());
		this.unbalancedReturnSite = stmt;
	}
	
	
	@Override
	public String toString() {
		return super.toString() + " returns via " + unbalancedReturnSite;
	}
}
