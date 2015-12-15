package wpds.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

import com.google.common.base.Joiner;

public abstract class WeightedPAutomaton<N extends Location, D extends State, W extends Weight>
    extends PAutomaton<N, D, W> {
  private Map<Transition<N, D>, W> transitionToWeights = new HashMap<>();

  public WeightedPAutomaton(D initialState, Set<Transition<N, D>> transitions, D finalState) {
    super(initialState, transitions, finalState);
  }


  public void addWeightForTransition(Transition<N, D> trans, W weight) {
    transitionToWeights.put(trans, weight);
  }

  public W getWeightFor(Transition<N, D> trans) {
    return transitionToWeights.get(trans);
  }



  @Override
  public String toString() {
    String s = "Weighted" + super.toString();
    s += "\n\tTransitionsToWeight\n\t\t";
    s += Joiner.on("\n\t\t").join(transitionToWeights.entrySet());
    return s;
  }


}
