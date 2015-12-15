package main;

import java.util.Set;

import soot.jimple.Jimple;
import wpds.impl.Transition;
import wpds.impl.WeightedPAutomaton;
import data.Fact;
import data.PDSSet;
import data.Stmt;

public class WPAutomaton extends WeightedPAutomaton<Stmt, Fact, PDSSet> {

  Stmt epsilon;

  public WPAutomaton(Fact initialState, Set<Transition<Stmt, Fact>> transitions, Fact finalState) {
    super(initialState, transitions, finalState);
  }

  @Override
  public Fact createState(Fact d, Stmt loc) {
    return new Fact(d, loc);
  }



  @Override
  public Stmt epsilon() {
    if (epsilon == null)
      epsilon = new Stmt(Jimple.v().newNopStmt());
    return epsilon;
  }

}
