package boomerang;

import boomerang.jimple.Statement;
import soot.Value;
import soot.jimple.AssignStmt;
import sync.pds.solver.nodes.Node;

public class AllocAtStmt extends Node<Statement, Value> {

	private AssignStmt fieldWriteStmt;

	public AllocAtStmt(Node<Statement, Value> target, AssignStmt fieldWriteStmt) {
		super(target.stmt(), target.fact());
		this.fieldWriteStmt = fieldWriteStmt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((fieldWriteStmt == null) ? 0 : fieldWriteStmt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AllocAtStmt other = (AllocAtStmt) obj;
		if (fieldWriteStmt == null) {
			if (other.fieldWriteStmt != null)
				return false;
		} else if (!fieldWriteStmt.equals(other.fieldWriteStmt))
			return false;
		return true;
	}

}
