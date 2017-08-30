package boomerang.poi;

import java.util.Set;

import com.beust.jcommander.internal.Sets;

import boomerang.ForwardQuery;
import sync.pds.solver.nodes.Node;

public abstract class AbstractPOI<Statement, Val, Field> implements PointOfIndirection<Statement, Val, Field> {

	private final Node<Statement,Val> node;
	private Set<ForwardQuery> actualBaseAllocations = Sets.newHashSet();
	private Set<ForwardQuery> flowAllocations = Sets.newHashSet();
	
	public AbstractPOI(Node<Statement,Val> node) {
		this.node = node;
	}

	public abstract void execute(ForwardQuery baseAllocation, ForwardQuery flowAllocation);

	@Override
	public void addBaseAllocation(ForwardQuery baseAllocation) {
		if(actualBaseAllocations.add(baseAllocation)){
			for(ForwardQuery flowAllocation : flowAllocations){
				execute(baseAllocation, flowAllocation);
			}
		}
	}

	@Override
	public void addFlowAllocation(ForwardQuery flowAllocation) {
		if(flowAllocations.add(flowAllocation)){
			for(ForwardQuery baseAllocation : actualBaseAllocations){
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
