package sync.pds.solver;

import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;
import wpds.interfaces.Location;

public class OneWeightFunctions<Stmt, Fact, Field> implements WeightFunctions<Stmt, Fact, Field> {

	private final Weight one;

	public OneWeightFunctions(Weight one){
		this.one = one;
	}
	@Override
	public Weight push(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ, Field field) {
		return one;
	}

	@Override
	public Weight normal(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ) {
		return one;
	}

	@Override
	public Weight pop(Node<Stmt, Fact> curr, Field location) {
		return one;
	}

};