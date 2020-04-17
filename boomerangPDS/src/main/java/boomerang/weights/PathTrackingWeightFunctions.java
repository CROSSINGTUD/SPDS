package boomerang.weights;

import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.weights.PathConditionWeight.ConditionDomain;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;

public class PathTrackingWeightFunctions
    implements WeightFunctions<Statement, Val, Statement, DataFlowPathWeight> {

  private boolean trackDataFlowPath;
  private boolean trackPathConditions;
  private boolean implicitBooleanCondition;

  public PathTrackingWeightFunctions(
      boolean trackDataFlowPath, boolean trackPathConditions, boolean implicitBooleanCondition) {
    this.trackDataFlowPath = trackDataFlowPath;
    this.trackPathConditions = trackPathConditions;
    this.implicitBooleanCondition = implicitBooleanCondition;
  }

  @Override
  public DataFlowPathWeight push(
      Node<Statement, Val> curr, Node<Statement, Val> succ, Statement callSite) {
    if (trackDataFlowPath && !curr.fact().isStatic()) {
      if (callSite.uses(curr.fact())) {
        if (implicitBooleanCondition && callSite.unwrap().isAssign()) {
          return new DataFlowPathWeight(
              new Node<>(callSite, curr.fact()), callSite, succ.stmt().getMethod());
        }
        return new DataFlowPathWeight(new Node<>(callSite, curr.fact()));
      }
      if (implicitBooleanCondition && callSite.unwrap().isAssign()) {
        return new DataFlowPathWeight(callSite, succ.stmt().getMethod());
      }
    }
    return DataFlowPathWeight.one();
  }

  @Override
  public DataFlowPathWeight normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
    if (trackDataFlowPath
        && curr.stmt().getMethod().getControlFlowGraph().getStartPoints().contains(curr.stmt())) {
      return new DataFlowPathWeight(curr);
    }
    if (trackDataFlowPath && !curr.fact().equals(succ.fact())) {
      return new DataFlowPathWeight(succ);
    }
    if (trackDataFlowPath
        && succ.stmt().isReturnStmt()
        && succ.stmt().getReturnOp().equals(succ.fact())) {
      return new DataFlowPathWeight(succ);
    }
    if (implicitBooleanCondition
        && curr.stmt().isAssign()
        && curr.stmt().getLeftOp().getType().isBooleanType()) {
      return new DataFlowPathWeight(
          curr.stmt().getLeftOp(),
          curr.stmt().getRightOp().toString().equals("0")
              ? ConditionDomain.FALSE
              : ConditionDomain.TRUE);
    }

    if (implicitBooleanCondition && succ.stmt().isReturnStmt()) {
      return new DataFlowPathWeight(succ.stmt().getReturnOp());
    }

    if (trackPathConditions && curr.stmt().isIfStmt()) {
      if (curr.stmt().getIfStmt().getTarget().equals(succ.stmt())) {
        return new DataFlowPathWeight(curr.stmt(), true);
      }
      return new DataFlowPathWeight(curr.stmt(), false);
    }
    return DataFlowPathWeight.one();
  }

  @Override
  public DataFlowPathWeight pop(Node<Statement, Val> curr) {
    return DataFlowPathWeight.one();
  }

  @Override
  public DataFlowPathWeight getOne() {
    return DataFlowPathWeight.one();
  }
}
