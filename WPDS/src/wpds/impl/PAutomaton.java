package wpds.impl;

import java.util.Collection;
import java.util.Set;

import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public abstract class PAutomaton<N extends Location, D extends State, W extends Weight> {
  // Set Q is implicit
  // Weighted Pushdown Systems and their Application to Interprocedural Dataflow Analysis
  protected Set<Transition<N, D, W>> transitions = Sets.newHashSet();
  // set F in paper [Reps2003]
  protected Set<D> finalStates;
  // set P in paper [Reps2003]
  protected Set<D> initialStates;
  private final Multimap<D, Transition<N, D, W>> transitionsOutOf = HashMultimap.create();
  private final Multimap<D, Transition<N, D, W>> transitionsInto = HashMultimap.create();

  public PAutomaton(Set<D> initialStates, Set<Transition<N, D, W>> transitions, Set<D> finalStates) {
    this.initialStates = Sets.newHashSet(initialStates);
    this.transitions = Sets.newHashSet(transitions);
    this.finalStates = Sets.newHashSet(finalStates);
    initTransitions();
  }

  private void initTransitions() {
    for (Transition<N, D, W> trans : transitions) {
      addTransition(trans);
    }
  }

  public abstract D createState(D d, N loc);

  public abstract WeightedPAutomaton<N, D, W> copy();

  public Set<Transition<N, D, W>> getTransitions() {
    return Sets.newHashSet(transitions);
  }



  public Collection<Transition<N, D, W>> getTransitionsOutOf(D state) {
    return transitionsOutOf.get(state);
  }

  public Collection<Transition<N, D, W>> getTransitionsInto(D state) {
    return transitionsInto.get(state);
  }

  public boolean addTransition(Transition<N, D, W> trans) {
    transitionsOutOf.get(trans.getStart()).add(trans);
    transitionsInto.get(trans.getTarget()).add(trans);
    return transitions.add(trans);
  }



  public Set<D> getInitialStates() {
    return Sets.newHashSet(initialStates);
  }

  public Set<D> getFinalStates() {
    return Sets.newHashSet(finalStates);
  }

  public String toString() {
    String s = "PAutomaton\n";
    s += "\tInitialStates:" + initialStates + "\n";
    s += "\tFinalStates:" + finalStates + "\n";
    s += "\tTransitions:\n\t\t";
    s += Joiner.on("\n\t\t").join(transitions);
    return s;
  }


}
