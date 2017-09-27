package typestate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import heros.EdgeFunction;
import heros.edgefunc.AllBottom;
import heros.edgefunc.AllTop;
import heros.edgefunc.EdgeIdentity;
import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.IdentityTransition;
import typestate.finiteautomata.Transition;
import wpds.impl.Weight;

public class TransitionFunction<State> extends Weight implements EdgeFunction<TypestateDomainValue<State>> {

	private final Set<ITransition<State>> value;

	private final String rep;

	private static Logger logger = LoggerFactory.getLogger(TransitionFunction.class);

	private static TransitionFunction one;

	private static TransitionFunction zero;

	public TransitionFunction(Set<? extends ITransition<State>> trans) {
		this.value = new HashSet<>(trans);
		this.rep = null;
	}

	public TransitionFunction(ITransition<State> trans) {
		this(new HashSet<>(Collections.singleton(trans)));
	}

	private TransitionFunction(String rep) {
		this.value = null;
		this.rep = rep;
	}
	@Override
	public TypestateDomainValue<State> computeTarget(TypestateDomainValue<State> source) {
		Set<State> states = new HashSet<>();
		for (ITransition<State> t : value) {
			if (t instanceof IdentityTransition) {
				states.addAll(source.getStates());
				continue;
			}
			for (State sourceState : source.getStates()) {
				if (t.from().equals(sourceState)) {
					states.add(t.to());
				}
			}
		}
		return new TypestateDomainValue<State>(states);
	}

	@Override
	public EdgeFunction<TypestateDomainValue<State>> composeWith(EdgeFunction<TypestateDomainValue<State>> secondFunction) {
		if (secondFunction instanceof AllTop)
			return secondFunction;

		if (secondFunction instanceof AllBottom)
			return this;

		if (secondFunction instanceof EdgeIdentity) {
			return this;
		}
		if (!(secondFunction instanceof TransitionFunction))
			throw new RuntimeException("Wrong type, is of type: " + secondFunction);
		TransitionFunction<State> func = (TransitionFunction) secondFunction;
		Set<ITransition<State>> otherTransitions = func.value;
		Set<ITransition<State>> res = new HashSet<>();
		for (ITransition<State> first : value) {
			for (ITransition<State> second : otherTransitions) {
				if (second instanceof IdentityTransition) {
					res.add(first);
				} else if (first instanceof IdentityTransition) {
					res.add(second);
				} else if (first.to().equals(second.from()))
					res.add(new Transition<State>(first.from(), second.to()));
			}
		}
		logger.debug("ComposeWith: {} with {} -> {}", this, secondFunction, new TransitionFunction(res));
		return new TransitionFunction<State>(res);

	}

	@Override
	public Weight extendWith(Weight other) {
		if (other.equals(one()))
			return this;
		if(this.equals(one()))
			return other;
		if(other.equals(zero()))
			return zero();
		System.err.println(this);
		TransitionFunction<State> func = (TransitionFunction) other;
		Set<ITransition<State>> otherTransitions = func.value;
		Set<ITransition<State>> ress = new HashSet<>();
		for (ITransition<State> first : value) {
			for (ITransition<State> second : otherTransitions) {
				if (second instanceof IdentityTransition) {
					ress.add(first);
				} else if (first instanceof IdentityTransition) {
					ress.add(second);
				} else if (first.to().equals(second.from()))
					ress.add(new Transition<State>(first.from(), second.to()));
			}
		}
		return new TransitionFunction<State>(ress);
	}

	@Override
	public Weight combineWith(Weight other) {
		if(!(other instanceof TransitionFunction))
			throw new RuntimeException();
		if(this.equals(one()))
			return other;
		if (other.equals(one())) {
			Set<ITransition<State>> transitions = new HashSet<>(value);
			transitions.add(new IdentityTransition<State>());
			return new TransitionFunction<State>(transitions);
		}
		if(other.equals(zero()))
			return this;
		TransitionFunction<State> func = (TransitionFunction) other;
		Set<ITransition<State>> transitions =  new HashSet<>(func.value);
		transitions.addAll(value);
		return new TransitionFunction<State>(transitions);
	};

	public static <State> TransitionFunction<State> one() {
		if(one == null)
			one = new TransitionFunction<State>("ONE");
		return one;
	}

	public static <State> TransitionFunction<State> zero() {
		if(zero == null)
			zero = new TransitionFunction<State>("ZERO");
		return zero;
	}
	@Override
	public EdgeFunction<TypestateDomainValue<State>> joinWith(EdgeFunction<TypestateDomainValue<State>> otherFunction) {
		if (otherFunction instanceof AllTop)
			return this;
		if (otherFunction instanceof AllBottom)
			return otherFunction;
		if (otherFunction instanceof EdgeIdentity) {
			Set<ITransition<State>> transitions = new HashSet<>(value);
			transitions.add(new IdentityTransition<State>());
			return new TransitionFunction<State>(transitions);
		}
		TransitionFunction<State> func = (TransitionFunction) otherFunction;
		Set<ITransition<State>> transitions =  new HashSet<>(func.value);
		transitions.addAll(value);
		return new TransitionFunction<State>(transitions);
	}

	@Override
	public boolean equalTo(EdgeFunction<TypestateDomainValue<State>> other) {
		if (!(other instanceof TransitionFunction))
			return false;
		TransitionFunction<State> func = (TransitionFunction) other;
		return func.value.equals(value);
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
