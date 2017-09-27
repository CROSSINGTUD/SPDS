package typestate.finiteautomata;

public interface State {
  public boolean isErrorState();

  public boolean isInitialState();
}
