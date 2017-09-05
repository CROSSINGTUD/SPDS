package boomerang.poi;

import java.util.Set;

import com.beust.jcommander.internal.Sets;

import boomerang.ForwardQuery;
import boomerang.Query;
import sync.pds.solver.nodes.Node;

public abstract class AbstractPOI<Statement, Val, Field> implements PointOfIndirection<Statement, Val, Field> {

	private final Node<Statement,Val> node;
	private Set<Query> actualBaseAllocations = Sets.newHashSet();
	private Set<Query> flowAllocations = Sets.newHashSet();
	
	public AbstractPOI(Node<Statement,Val> node) {
		this.node = node;
	}

	public abstract void execute(Query baseAllocation, Query flowAllocation);

	@Override
	public void addBaseAllocation(Query baseAllocation) {
		if(actualBaseAllocations.add(baseAllocation)){
			for(Query flowAllocation : flowAllocations){
				execute(baseAllocation, flowAllocation);
			}
		}
	}

	@Override
	public void addFlowAllocation(Query flowAllocation) {
		if(flowAllocations.add(flowAllocation)){
			for(Query baseAllocation : actualBaseAllocations){
				execute(baseAllocation, flowAllocation);
			}
		}
	}
	
	

	@Override
	public Node<Statement,Val> getNode() {
		return node;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractPOI other = (AbstractPOI) obj;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		return true;
	}
}
