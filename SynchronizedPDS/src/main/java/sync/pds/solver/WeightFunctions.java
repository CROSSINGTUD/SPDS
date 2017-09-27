package sync.pds.solver;

import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;
import wpds.interfaces.Location;

public interface WeightFunctions<Stmt,Fact,Field, W extends Weight> {
	public W push(Node<Stmt,Fact> curr, Node<Stmt,Fact> succ, Field field);
	public W normal(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ);
	public W pop(Node<Stmt, Fact> curr, Field location);
	public W getOne();
	public W getZero();
}
