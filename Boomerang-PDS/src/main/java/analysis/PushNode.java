package analysis;

import analysis.Solver.PDSSystem;

public class PushNode<Stmt,Fact,Location> extends PopNode<Stmt,Fact,Location>{

	public PushNode(Stmt stmt, Fact variable, Location loc, PDSSystem system) {
		super(stmt, variable, loc, system);
	}
	
}
