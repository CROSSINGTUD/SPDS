package sync.pds.solver.nodes;


public class SingleNode<Fact> implements INode<Fact>{
	private Fact fact;
	private int hashCode = 0;

	public SingleNode(Fact fact){
		this.fact = fact;
	}

	@Override
	public int hashCode() {
		if(hashCode != 0)
			return hashCode;
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fact == null) ? 0 : fact.hashCode());
		hashCode = result;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SingleNode other = (SingleNode) obj;
		if (fact == null) {
			if (other.fact != null)
				return false;
		} else if (!fact.equals(other.fact))
			return false;
		return true;
	}

	@Override
	public Fact fact() {
		return fact;
	}
	
	@Override
	public String toString() {
		return fact.toString();
	}
}