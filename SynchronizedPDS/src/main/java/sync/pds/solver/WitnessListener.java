package sync.pds.solver;

import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.interfaces.Location;

public interface WitnessListener<Stmt extends Location,Fact,Field extends Location> {

	void fieldWitness(Transition<Field, INode<Node<Stmt, Fact>>> transition);

	void callWitness(Transition<Stmt, INode<Fact>> t);

}
