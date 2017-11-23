package typestate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.State;
import typestate.finiteautomata.Transition;
import wpds.impl.Weight;

public class TransitionFunction extends Weight {

	private final Set<ITransition> value;

	private final String rep;

	private static TransitionFunction one;

	private static TransitionFunction zero;

	public TransitionFunction(Set<? extends ITransition> trans) {
		this.value = new HashSet<>(trans);
		this.rep = null;
	}

	public TransitionFunction(ITransition trans) {
		this(new HashSet<>(Collections.singleton(trans)));
	}

	private TransitionFunction(String rep) {
		this.value = Sets.newHashSet();
		this.rep = rep;
	}

	public Collection<ITransition> values(){
		return Lists.newArrayList(value);
	}
	
	@Override
	public Weight extendWith(Weight other) {
		if (other.equals(one()))
			return this;
		if(this.equals(one()))
			return other;
		if(other.equals(zero()) || this.equals(zero())){
			return zero();
		}
		TransitionFunction func = (TransitionFunction) other;
		Set<ITransition> otherTransitions = func.value;
		Set<ITransition> ress = new HashSet<>();
		for (ITransition first : value) {
			for (ITransition second : otherTransitions) {
				if (second.equals(Transition.identity())) {
					ress.add(first);
				} else if (first.equals(Transition.identity())) {
					ress.add(second);
				} else if (first.to().equals(second.from())){
					ress.add(new Transition(first.from(), second.to()));
				}
			}
		}
		if(ress.isEmpty()){
			return zero();
		}
		return new TransitionFunction(ress);
	}

	@Override
	public Weight combineWith(Weight other) {
		if(!(other instanceof TransitionFunction))
			throw new RuntimeException();
		if(this.equals(zero()))
			return other;
		if(other.equals(zero()))
			return this;
		if (other.equals(one()) && this.equals(one())) {
			return one();
		}
		TransitionFunction func = (TransitionFunction) other;
		if (other.equals(one()) || this.equals(one())) {
			Set<ITransition> transitions = new HashSet<>((other.equals(one()) ? value : func.value));
			transitions.add(Transition.identity());
			return new TransitionFunction(transitions);
		}
		Set<ITransition> transitions = new HashSet<>(func.value);
		transitions.addAll(value);
		return new TransitionFunction(transitions);
	};

	public static TransitionFunction one() {
		if(one == null)
			one = new TransitionFunction("ONE");
		return one;
	}

	public static  TransitionFunction zero() {
		if(zero == null)
			zero = new TransitionFunction("ZERO");
		return zero;
	}

	public String toString() {
		if(this.rep != null)
			return this.rep;
		return "{Func:" + value.toString() + "}";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rep == null) ? 0 : rep.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		TransitionFunction other = (TransitionFunction) obj;
		if (rep == null) {
			if (other.rep != null)
				return false;
		} else if (!rep.equals(other.rep))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}
