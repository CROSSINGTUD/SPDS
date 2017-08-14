package boomerang.jimple;

import com.google.common.base.Optional;

import soot.Unit;
import soot.jimple.Stmt;
import wpds.interfaces.Location;

public class Statement implements Location {
	private static Statement epsilon;
	private Stmt delegate;
	private String rep;

	public Statement(Stmt delegate){
		this.delegate = delegate;
	}
	private Statement(String rep){
		this.rep = rep;
		this.delegate = null;
	}
	
	public Optional<Stmt> getUnit(){
		if(delegate == null)
			return Optional.absent();
		return Optional.of(delegate);
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
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
		Statement other = (Statement) obj;
		if (delegate == null) {
			if (other.delegate != null)
				return false;
		} else if (!delegate.equals(other.delegate))
			return false;
		return true;
	}
	


	public static Statement epsilon() {
		if (epsilon == null){
			epsilon = new Statement("eps_s");
		}
		return epsilon;
	}
	@Override
	public String toString() {
		if(delegate == null){
			return rep;
		}
		return delegate.toString();
	}
	
}
