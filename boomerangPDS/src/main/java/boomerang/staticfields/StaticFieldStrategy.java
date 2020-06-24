package boomerang.staticfields;

import boomerang.scene.Statement;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Val;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import java.util.Set;
import wpds.impl.Weight;
import wpds.interfaces.State;

public interface StaticFieldStrategy<W extends Weight> {
  void handleForward(
      Statement storeStmt,
      Val storedVal,
      StaticFieldVal staticVal,
      Set<State> out,
      ForwardBoomerangSolver<W> solver);

  void handleBackward(
      Statement curr,
      Val leftOp,
      StaticFieldVal staticField,
      Statement succ,
      Set<State> out,
      BackwardBoomerangSolver<W> solver);
}
