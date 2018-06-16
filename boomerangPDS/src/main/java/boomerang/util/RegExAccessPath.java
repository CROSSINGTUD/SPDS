package boomerang.util;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import pathexpression.IRegEx;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;

public class RegExAccessPath {
	private final Val val;
	private final IRegEx<Field> fields;
	private final INode<Node<Statement, Val>> start;
	private final INode<Node<Statement, Val>> target;

	public RegExAccessPath(Val val, INode<Node<Statement, Val>> start, IRegEx<Field> fields, INode<Node<Statement, Val>> target){
		this.val = val;
		this.start = start;
		this.fields = fields;
		this.target = target;
	}

	public Val getVal(){
		return val;
	}
	
	public IRegEx<Field> getFields() {
		return fields;
	}
	
	@Override
	public String toString() {
		return val.value() + " " + fields.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		result = prime * result + ((val == null) ? 0 : val.hashCode());
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
		RegExAccessPath other = (RegExAccessPath) obj;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		if (val == null) {
			if (other.val != null)
				return false;
		} else if (!val.equals(other.val))
			return false;
		return true;
	}
}
