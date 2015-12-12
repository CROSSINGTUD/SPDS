package main;

import java.util.Set;

import wpds.impl.Transition;
import wpds.impl.WeightedPAutomaton;
import data.Access;
import data.Fact;
import data.Stmt;

public class WPAutomaton extends WeightedPAutomaton<Stmt, Fact, Access> {

  public WPAutomaton(Set<Fact> initialStates, Set<Transition<Stmt, Fact, Access>> transitions,
      Set<Fact> finalStates) {
    super(initialStates, transitions, finalStates);
    // TODO Auto-generated constructor stub
  }

  @Override
  public Fact createState(Fact d, Stmt loc) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public WeightedPAutomaton<Stmt, Fact, Access> copy() {
    return new WPAutomaton(initialStates, transitions, finalStates);
  }


}
