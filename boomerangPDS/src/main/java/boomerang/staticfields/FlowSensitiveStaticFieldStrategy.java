package boomerang.staticfields;

import boomerang.scene.Statement;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Val;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import java.util.Set;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;
import wpds.interfaces.State;

public class FlowSensitiveStaticFieldStrategy<W extends Weight> implements StaticFieldStrategy<W> {
  @Override
  public void handleForward(
      Statement storeStmt,
      Val storedVal,
      StaticFieldVal staticVal,
      Set<State> out,
      ForwardBoomerangSolver<W> solver) {
    out.add(new Node<Statement, Val>(storeStmt, staticVal));
  }

  @Override
  public void handleBackward(
      Statement loadStatement,
      Val loadedVal,
      StaticFieldVal staticVal,
      Statement succ,
      Set<State> out,
      BackwardBoomerangSolver<W> solver) {
    out.add(new Node<Statement, Val>(succ, loadStatement.getStaticField()));
  }
}
