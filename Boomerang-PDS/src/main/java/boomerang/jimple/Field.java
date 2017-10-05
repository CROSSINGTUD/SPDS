package boomerang.jimple;

import java.util.Map;

import com.beust.jcommander.internal.Maps;

import soot.SootField;
import wpds.interfaces.Empty;
import wpds.interfaces.Location;
import wpds.wildcard.ExclusionWildcard;
import wpds.wildcard.Wildcard;

public class Field implements Location {
	private static Field wildcard;
	private static Field epsilon;
	private static Field empty;
	private static Field array;
	private final SootField delegate;
	private final String rep;

	public Field(SootField delegate) {
		this.delegate = delegate;
		this.rep = null;
	}

	private Field(String rep) {
		this.rep = rep;
		this.delegate = null;
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
		if(rep != null)
			return false;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Field other = (Field) obj;
		if(other.rep != null)
			return false;
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

	@Override
	public String toString() {
		if (delegate == null)
			return rep;
		return delegate.getName().toString();
	}

	public static Field wildcard() {
		if (wildcard == null){
			wildcard = new WildcardField();
		}
		return wildcard;
	}

	public static Field empty() {
		if (empty == null) {
			empty = new EmptyField("{}");
		}
		return empty;
	}
	
	private static class EmptyField extends Field implements Empty{
		public EmptyField(String rep) {
			super(rep);
		}
	}

	public static Field epsilon() {
		if (epsilon == null) {
			epsilon = new EmptyField("eps_f");
		}
		return epsilon;
	}
	
	public static Field array() {
		if (array == null) {
			array = new Field("array");
		}
		return array;
	}
	private static class WildcardField extends Field implements Wildcard {
		public WildcardField() {
			super("*");
		}
	}
	
	private static class ExclusionWildcardField extends Field implements ExclusionWildcard<Field> {
		private final Field excludes;

		public ExclusionWildcardField(Field excl) {
			super(excl.delegate);
			this.excludes = excl;
		}

		@Override
		public Field excludes() {
			return (Field) excludes;
		}
		@Override
		public String toString() {
			return "not " + super.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((excludes == null) ? 0 : excludes.hashCode());
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
			ExclusionWildcardField other = (ExclusionWildcardField) obj;
			if (excludes == null) {
				if (other.excludes != null)
					return false;
			} else if (!excludes.equals(other.excludes))
				return false;
			return true;
		}
		
		
	}
	private static Map<Field,ExclusionWildcardField> exclusionWildcards = Maps.newHashMap();
	
	public static Field exclusionWildcard(Field exclusion) {
		if(!exclusionWildcards.containsKey(exclusion)){
			exclusionWildcards.put(exclusion, new ExclusionWildcardField(exclusion));
		}
		return exclusionWildcards.get(exclusion);
	}
}
