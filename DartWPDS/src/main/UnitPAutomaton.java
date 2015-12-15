package main;

import java.util.Set;

import data.Fact;
import data.PDSSet;
import soot.Unit;
import soot.jimple.Jimple;
import wpds.impl.Transition;
import wpds.impl.WeightedPAutomaton;

public class UnitPAutomaton extends WeightedPAutomaton<Unit, Fact, PDSSet> {

  Unit epsilon;

  public UnitPAutomaton(Fact initialState, Set<Transition<Unit, Fact>> transitions,
      Fact finalState) {
    super(initialState, transitions, finalState);
  }

  @Override
  public Fact createState(Fact d, Unit loc) {
    return new Fact(d, loc);
  }



  @Override
  public Unit epsilon() {
    if (epsilon == null)
      epsilon = Jimple.v().newNopStmt();
    return epsilon;
  }

}
