package typestate.finiteautomata;

public class IdentityTransition implements ITransition {

	private static IdentityTransition instance;

	private IdentityTransition() {
	}

	@Override
	public State from() {
		throw new RuntimeException("Unreachable");
	}

	@Override
	public State to() {
		throw new RuntimeException("Unreachable");
	}

	@Override
	public String toString() {
		return "ID -> ID";
	}
	
	public static IdentityTransition v(){
		if(instance == null)
			instance = new IdentityTransition();
		return instance;
	}
}
