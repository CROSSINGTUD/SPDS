package boomerang.staticfields;

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Val;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import java.util.Set;
import wpds.impl.Weight;
import wpds.interfaces.State;

public class IgnoreStaticFieldStrategy<W extends Weight> implements StaticFieldStrategy<W> {

  @Override
  public void handleForward(
      Edge storeStmt,
      Val storedVal,
      StaticFieldVal staticVal,
      Set<State> out,
      ForwardBoomerangSolver<W> solver) {}

  @Override
  public void handleBackward(
      Edge loadStatement,
      Val loadedVal,
      StaticFieldVal staticVal,
      Set<State> out,
      BackwardBoomerangSolver<W> solver) {}
}
