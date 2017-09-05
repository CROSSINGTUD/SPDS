package boomerang.poi;

import boomerang.Query;
import sync.pds.solver.nodes.Node;

public interface PointOfIndirection<Statement, Val, Field> {
	public void addBaseAllocation(Query baseAllocation);
	public void addFlowAllocation(Query flowAllocation);
	public Node<Statement,Val> getNode();
}
