package wpds.impl;

import heros.demandide.pathexpression.Edge;
import heros.demandide.pathexpression.LabeledGraph;
import heros.demandide.pathexpression.PathExpressionComputer;

import java.util.Collection;
import java.util.Set;

import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public abstract class PAutomaton<N extends Location, D extends State, W extends Weight> implements
    LabeledGraph<D, N> {
  // Set Q is implicit
  // Weighted Pushdown Systems and their Application to Interprocedural Dataflow Analysis
  protected Set<Transition<N, D, W>> transitions = Sets.newHashSet();
  // set F in paper [Reps2003]
  protected D finalState;
  // set P in paper [Reps2003]
  protected D initialState;
  protected Set<D> states = Sets.newHashSet();
  private final Multimap<D, Transition<N, D, W>> transitionsOutOf = HashMultimap.create();
  private final Multimap<D, Transition<N, D, W>> transitionsInto = HashMultimap.create();

  public PAutomaton(D initialState, Set<Transition<N, D, W>> transitions, D finalState) {
    this.initialState = initialState;
    this.transitions = Sets.newHashSet(transitions);
    this.finalState = finalState;
    initTransitions();
  }

  private void initTransitions() {
    for (Transition<N, D, W> trans : transitions) {
      addTransition(trans);
    }
  }

  public abstract D createState(D d, N loc);


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
    states.add(trans.getTarget());
    states.add(trans.getStart());
    return transitions.add(trans);
  }



  public D getInitialState() {
    return initialState;
  }

  public D getFinalState() {
    return finalState;
  }



  public String toString() {
    String s = "PAutomaton\n";
    s += "\tInitialStates:" + initialState + "\n";
    s += "\tFinalStates:" + finalState + "\n";
    s += "\tTransitions:\n\t\t";
    s += Joiner.on("\n\t\t").join(transitions);
    return s;
  }


  public abstract N epsilon();

  public String extractLanguage(D from) {
    PathExpressionComputer<D, N> expr = new PathExpressionComputer<>(this);
    return expr.getExpressionBetween(from, getFinalState()).toString();
  }

  public Set<D> getStates() {
    return states;
  }

  public Set<Edge<D, N>> getEdges() {
    Set<Edge<D, N>> trans = Sets.newHashSet();
    for (Edge<D, N> tran : transitions)
      trans.add(tran);
    return trans;
  };

  public Set<D> getNodes() {
    return getStates();
  };


}
