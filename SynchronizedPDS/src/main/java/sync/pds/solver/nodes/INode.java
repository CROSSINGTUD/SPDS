package sync.pds.solver.nodes;

import wpds.interfaces.State;

public interface INode<Fact> extends State {
	public Fact fact();
}
