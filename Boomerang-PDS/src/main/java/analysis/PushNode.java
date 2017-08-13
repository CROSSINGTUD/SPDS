package analysis;

import analysis.Solver.PDSSystem;

public class PushNode<Stmt,Fact,Location> extends Node<Stmt,Fact>{

	private PDSSystem system;
	private Location location;
	public PushNode(Stmt stmt, Fact variable, Location location, PDSSystem system) {
		super(stmt, variable);
		this.system = system;
		this.location = location;
	}
	
	public PDSSystem system(){
		return system;
	}
	

	public Location location(){
		return location;
	}
	@Override
	public String toString() {
		return super.toString() + " Push " + location;
	}
}
