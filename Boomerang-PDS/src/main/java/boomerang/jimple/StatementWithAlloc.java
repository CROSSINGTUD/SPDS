package boomerang.jimple;

public class StatementWithAlloc extends Statement {

	private Statement alloc;

	public StatementWithAlloc(Statement stmt, Statement alloc) {
		super(stmt.getUnit().get(), stmt.getMethod());
		this.alloc = alloc;
	}
	
	public Statement getAlloc() {
		return alloc;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((alloc == null) ? 0 : alloc.hashCode());
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
		StatementWithAlloc other = (StatementWithAlloc) obj;
		if (alloc == null) {
			if (other.alloc != null)
				return false;
		} else if (!alloc.equals(other.alloc))
			return false;
		return true;
	}

}
