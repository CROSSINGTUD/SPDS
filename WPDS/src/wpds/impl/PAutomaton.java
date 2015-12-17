package wpds.impl;

import java.util.Set;

import pathexpression.LabeledGraph;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public abstract class PAutomaton<N extends Location, D extends State>
    extends WeightedPAutomaton<N, D, NoWeight<N>>
    implements LabeledGraph<D, N> {

  public PAutomaton(D initialState, Set<Transition<N, D>> transitions, D finalState) {
    super(initialState, transitions, finalState);
  }



}
