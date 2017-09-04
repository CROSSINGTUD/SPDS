package sync.pds.solver.nodes;


public class GeneratedState<L,N> implements INode<L>{
	
	private INode<L> node;
	private N loc;


	public GeneratedState(INode<L> node, N loc) {
		this.node = node;
		this.loc = loc;
	}
	@Override
	public L fact() {
		throw new RuntimeException("System internal state");
	}
	
	public N location(){
		return loc;
	}

	@Override
	public String toString() {
		return node + " " + loc;
	}
}