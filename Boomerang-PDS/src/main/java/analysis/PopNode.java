package analysis;

import analysis.Solver.PDSSystem;

public class PopNode<Stmt,Fact,Location> extends Node<Stmt,Fact>{

	private PDSSystem system;
	private Location location;
	public PopNode(Stmt stmt, Fact variable, Location location, PDSSystem system) {
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


}
