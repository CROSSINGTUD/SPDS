package analysis;

import wpds.interfaces.State;

public interface INode<Stmt, Fact> extends State {
	public Stmt stmt();
	
	public Fact fact();
}
