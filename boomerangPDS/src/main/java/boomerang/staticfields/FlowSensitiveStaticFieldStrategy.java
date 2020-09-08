package boomerang.staticfields;

import boomerang.scene.ControlFlowGraph.Edge;
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
      Edge storeStmt,
      Val storedVal,
      StaticFieldVal staticVal,
      Set<State> out,
      ForwardBoomerangSolver<W> solver) {
    out.add(new Node<>(storeStmt, staticVal));
  }

  @Override
  public void handleBackward(
      Edge loadStatement,
      Val loadedVal,
      StaticFieldVal staticVal,
      Set<State> out,
      BackwardBoomerangSolver<W> solver) {
    out.add(new Node<>(loadStatement, loadStatement.getStart().getStaticField()));
  }
}
