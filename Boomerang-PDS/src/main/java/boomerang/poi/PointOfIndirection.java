package boomerang.poi;

import boomerang.ForwardQuery;
import sync.pds.solver.nodes.Node;

public interface PointOfIndirection<Statement, Val, Field> {
	public void addBaseAllocation(ForwardQuery baseAllocation);
	public void addFlowAllocation(ForwardQuery flowAllocation);
	public Node<Statement,Val> getNode();
}
