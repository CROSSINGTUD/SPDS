package boomerang.staticfields;

import boomerang.scene.Statement;
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
      Statement storeStmt,
      Val storedVal,
      StaticFieldVal staticVal,
      Set<State> out,
      ForwardBoomerangSolver<W> solver) {}

  @Override
  public void handleBackward(
      Statement loadStatement,
      Val loadedVal,
      StaticFieldVal staticVal,
      Statement succ,
      Set<State> out,
      BackwardBoomerangSolver<W> solver) {}
}
