package analysis;

import wpds.interfaces.State;

public interface INode<Fact> extends State {
	public Fact fact();
}
