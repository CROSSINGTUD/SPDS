package boomerang.poi;

import boomerang.ForwardQuery;
import boomerang.Query;
import sync.pds.solver.nodes.Node;

public interface PointOfIndirection<Statement, Val, Field> {
	public void addBaseAllocation(ForwardQuery baseAllocation);
	public void addFlowAllocation(Query flowAllocation);
	public Val getBaseVar();
	public Val getStoredVar();
	public Field getField();
	public Statement getStmt();
}
