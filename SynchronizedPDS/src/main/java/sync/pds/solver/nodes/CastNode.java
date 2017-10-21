package sync.pds.solver.nodes;

public class CastNode<Statement,Val, Type> extends Node<Statement,Val> {
	private final Type type;

	public CastNode(Statement stmt, Val variable, Type type) {
		super(stmt, variable);
		this.type = type;
	}
	
	public Type getType(){
		return type;
	}
}
