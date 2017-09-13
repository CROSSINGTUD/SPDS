package boomerang.poi;

import java.util.Set;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;

import boomerang.ForwardQuery;
import boomerang.Query;

public abstract class PointOfIndirection<Statement, Val, Field> {

	private Set<ForwardQuery> actualBaseAllocations = Sets.newHashSet();
	private Set<Query> flowAllocations = Sets.newHashSet();
	public abstract void execute(ForwardQuery baseAllocation, Query flowAllocation);

	public void addBaseAllocation(ForwardQuery baseAllocation) {
		if(actualBaseAllocations.add(baseAllocation)){
			for(Query flowAllocation : Lists.newArrayList(flowAllocations)){
				execute(baseAllocation, flowAllocation);
			}
		}
	}

	public void addFlowAllocation(Query flowAllocation) {
		if(flowAllocations.add(flowAllocation)){
			for(ForwardQuery baseAllocation : Lists.newArrayList(actualBaseAllocations)){
				execute(baseAllocation, flowAllocation);
			}
		}
	}
	
	public abstract Statement getStmt();
}
