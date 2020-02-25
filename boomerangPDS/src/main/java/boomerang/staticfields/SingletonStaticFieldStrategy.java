package boomerang.staticfields;

import boomerang.WeightedBoomerang;
import boomerang.scene.Field;
import boomerang.scene.Statement;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Val;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import com.google.common.collect.Multimap;
import java.util.Set;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;
import wpds.interfaces.State;

public class SingletonStaticFieldStrategy<W extends Weight> implements StaticFieldStrategy<W> {

  private final Multimap<Field, Statement> fieldStoreStatements;
  private final Multimap<Field, Statement> fieldLoadStatements;

  public SingletonStaticFieldStrategy(WeightedBoomerang<W> boomerang) {
    this.fieldStoreStatements = boomerang.getCallGraph().getFieldStoreStatements();
    this.fieldLoadStatements = boomerang.getCallGraph().getFieldLoadStatements();
  }

  @Override
  public void handleForward(
      Statement storeStmt,
      Val storedVal,
      StaticFieldVal staticVal,
      Set<State> out,
      ForwardBoomerangSolver<W> solver) {
    for (Statement matchingStore : fieldLoadStatements.get(staticVal.field())) {
      solver.processNormal(
          new Node<Statement, Val>(storeStmt, storedVal),
          new Node<Statement, Val>(matchingStore, matchingStore.getLeftOp()));
    }
  }

  @Override
  public void handleBackward(
      Statement loadStatement,
      Val loadedVal,
      StaticFieldVal staticVal,
      Statement succ,
      Set<State> out,
      BackwardBoomerangSolver<W> solver) {
    for (Statement matchingStore : fieldStoreStatements.get(staticVal.field())) {
      solver.processNormal(
          new Node<Statement, Val>(loadStatement, loadedVal),
          new Node<Statement, Val>(matchingStore, matchingStore.getRightOp()));
    }
  }
}
