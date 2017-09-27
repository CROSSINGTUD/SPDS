package typestate.finiteautomata;

public class IdentityTransition<State> implements ITransition<State> {

	public IdentityTransition() {
	}

	public boolean equals(Object o) {
		return o instanceof IdentityTransition;
	}

	@Override
	public State from() {
		throw new RuntimeException("Unreachable");
	}

	@Override
	public State to() {
		throw new RuntimeException("Unreachable");
	}

}
