package boomerang.poi;

import java.util.Set;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;

import boomerang.ForwardQuery;
import boomerang.Query;
import sync.pds.solver.nodes.Node;

public abstract class AbstractPOI<Statement, Val, Field> implements PointOfIndirection<Statement, Val, Field> {

	private Set<Query> actualBaseAllocations = Sets.newHashSet();
	private Set<Query> flowAllocations = Sets.newHashSet();
	private final Val leftOp;
	private final Field field;
	private final Val rightOp;
	private Statement statement;
	
	public AbstractPOI(Statement statement, Val leftOp, Field field, Val rightOp) {
		this.statement = statement;
		this.leftOp = leftOp;
		this.field = field;
		this.rightOp = rightOp;
	}

	public abstract void execute(Query baseAllocation, Query flowAllocation);

	@Override
	public void addBaseAllocation(Query baseAllocation) {
		if(actualBaseAllocations.add(baseAllocation)){
			for(Query flowAllocation : Lists.newArrayList(flowAllocations)){
				execute(baseAllocation, flowAllocation);
			}
		}
	}

	@Override
	public void addFlowAllocation(Query flowAllocation) {
		if(flowAllocations.add(flowAllocation)){
			for(Query baseAllocation : Lists.newArrayList(actualBaseAllocations)){
				execute(baseAllocation, flowAllocation);
			}
		}
	}
	

	public Val getLeftOp() {
		return leftOp;
	}

	public Field getField() {
		return field;
	}

	public Val getRightOp() {
		return rightOp;
	}


	public Statement getStmt() {
		return statement;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((leftOp == null) ? 0 : leftOp.hashCode());
		result = prime * result + ((rightOp == null) ? 0 : rightOp.hashCode());
		result = prime * result + ((statement == null) ? 0 : statement.hashCode());
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
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (leftOp == null) {
			if (other.leftOp != null)
				return false;
		} else if (!leftOp.equals(other.leftOp))
			return false;
		if (rightOp == null) {
			if (other.rightOp != null)
				return false;
		} else if (!rightOp.equals(other.rightOp))
			return false;
		if (statement == null) {
			if (other.statement != null)
				return false;
		} else if (!statement.equals(other.statement))
			return false;
		return true;
	}


}
