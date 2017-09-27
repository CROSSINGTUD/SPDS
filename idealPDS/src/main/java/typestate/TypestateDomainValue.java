package typestate;

import java.util.HashSet;
import java.util.Set;

public class TypestateDomainValue<State> {

  private final Set<State> states;

  public TypestateDomainValue(Set<State> trans) {
    this.states = trans;
  }

  public TypestateDomainValue(State trans) {
	  this();
    this.states.add(trans);
  }
  public TypestateDomainValue() {
    this.states = new HashSet<>();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((states == null) ? 0 : states.hashCode());
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
    TypestateDomainValue other = (TypestateDomainValue) obj;
    if (states == null) {
      if (other.states != null)
        return false;
    } else if (!states.equals(other.states))
      return false;
    return true;
  }

  public Set<State> getStates() {
    return new HashSet<>(states);
  }

  @Override
  public String toString() {
    return states.toString();
  }
  
  public static <State> TypestateDomainValue<State> top(){
	  return TOP;
  }
  private  static final TypestateDomainValue TOP = new TypestateDomainValue() {
    public boolean equals(Object obj) {
      return obj == TOP;
    };

    public String toString() {
      return "TOP";
    };
  };
}
