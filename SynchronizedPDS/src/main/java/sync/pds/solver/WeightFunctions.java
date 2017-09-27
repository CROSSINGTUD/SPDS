package sync.pds.solver;

import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;
import wpds.interfaces.Location;

public interface WeightFunctions<Stmt,Fact,Field> {
	public Weight push(Node<Stmt,Fact> curr, Node<Stmt,Fact> succ, Field field);
	public Weight normal(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ);
	public Weight pop(Node<Stmt, Fact> curr, Field location);
}
