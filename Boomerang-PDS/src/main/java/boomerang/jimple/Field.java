package boomerang.jimple;

import soot.SootField;
import wpds.interfaces.Location;

public class Field implements Location{
	private static Field wildcard;
	private SootField delegate;
	private String rep;

	public Field(SootField delegate){
		this.delegate = delegate;
	}
	
	private Field(String rep){
		this.rep = rep;
		this.delegate = null;
	}
	
	public static Field wildcard(){
		if(wildcard == null)
			wildcard = new Field("*");
		return wildcard;
	}
	public static Field empty(){
		if(wildcard == null)
			wildcard = new Field("*");
		return wildcard;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
		result = prime * result + ((rep == null) ? 0 : rep.hashCode());
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
		Field other = (Field) obj;
		if (delegate == null) {
			if (other.delegate != null)
				return false;
		} else if (!delegate.equals(other.delegate))
			return false;
		if (rep == null) {
			if (other.rep != null)
				return false;
		} else if (!rep.equals(other.rep))
			return false;
		return true;
	}
}
