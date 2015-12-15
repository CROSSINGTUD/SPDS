package data;

import java.util.Set;

import wpds.impl.Transition;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.Weight.NoWeight;

public class FieldPAutomaton extends WeightedPAutomaton<WrappedSootField, AccessStmt, NoWeight> {

  public FieldPAutomaton(AccessStmt initialState,
      Set<Transition<WrappedSootField, AccessStmt>> transitions, AccessStmt finalState) {
    super(initialState, transitions, finalState);
  }

  @Override
  public AccessStmt createState(AccessStmt d, WrappedSootField loc) {
    return new AccessStmt(d, loc);
  }


  @Override
  public WrappedSootField epsilon() {
    return WrappedSootField.EPSILON;
  }

}
