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

  public WeightedPAutomaton(Set<D> initialStates, Set<Transition<N, D, W>> transitions,
      Set<D> finalStates) {
    super(initialStates, transitions, finalStates);
  }


  public void addWeightForTransition(Transition<N, D, W> trans, W weight) {
    transitionToWeights.put(trans, weight);
  }

  public W getWeightFor(Transition<N, D, W> trans) {
    return transitionToWeights.get(trans);
  }

  private Map<Transition<N, D, W>, W> transitionToWeights = new HashMap<>();


  @Override
  public String toString() {
    String s = "Weighted" + super.toString();
    s += "\n\tTransitionsToWeight\n\t\t";
    s += Joiner.on("\n\t\t").join(transitionToWeights.entrySet());
    return s;
  }
}
