package typestate.finiteautomata;

public interface ITransition<State> {
	public State from();
	public State to();
}
