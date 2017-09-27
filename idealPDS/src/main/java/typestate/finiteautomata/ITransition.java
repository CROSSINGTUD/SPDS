package typestate.finiteautomata;

public interface ITransition {
	public State from();
	public State to();
}
