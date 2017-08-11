package analysis;

public class NodeWithLocation<Stmt, Fact, Location> extends Node<Stmt, Fact>{

	private Location loc;

	public NodeWithLocation(Stmt stmt, Fact variable, Location loc) {
		super(stmt, variable);
		this.loc = loc;
	}
	
	public Node<Stmt,Fact> asNode(){
		return new Node<Stmt,Fact>(stmt,variable);
	}

	public Location location(){
		return loc;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((loc == null) ? 0 : loc.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeWithLocation other = (NodeWithLocation) obj;
		if (loc == null) {
			if (other.loc != null)
				return false;
		} else if (!loc.equals(other.loc))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return super.toString() + " loc: " + loc;
	}

}
