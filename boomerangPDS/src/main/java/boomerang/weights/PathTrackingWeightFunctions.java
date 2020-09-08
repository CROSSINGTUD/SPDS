package boomerang.weights;

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Val;
import boomerang.weights.PathConditionWeight.ConditionDomain;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;

public class PathTrackingWeightFunctions
    implements WeightFunctions<Edge, Val, Edge, DataFlowPathWeight> {

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
  public DataFlowPathWeight push(Node<Edge, Val> curr, Node<Edge, Val> succ, Edge callSite) {
    if (trackDataFlowPath && !curr.fact().isStatic()) {
      if (callSite.getStart().uses(curr.fact())) {
        if (implicitBooleanCondition && callSite.getTarget().isAssign()) {
          return new DataFlowPathWeight(
              new Node<>(callSite, curr.fact()), callSite.getStart(), succ.stmt().getMethod());
        }
        return new DataFlowPathWeight(new Node<>(callSite, curr.fact()));
      }
      if (implicitBooleanCondition && callSite.getStart().isAssign()) {
        return new DataFlowPathWeight(callSite.getStart(), succ.stmt().getMethod());
      }
    }
    return DataFlowPathWeight.one();
  }

  @Override
  public DataFlowPathWeight normal(Node<Edge, Val> curr, Node<Edge, Val> succ) {
    if (trackDataFlowPath
        && curr.stmt().getMethod().getControlFlowGraph().getStartPoints().contains(curr.stmt())) {
      return new DataFlowPathWeight(curr);
    }
    if (trackDataFlowPath && !curr.fact().equals(succ.fact())) {
      return new DataFlowPathWeight(succ);
    }
    if (trackDataFlowPath
        && succ.stmt().getTarget().isReturnStmt()
        && succ.stmt().getTarget().getReturnOp().equals(succ.fact())) {
      return new DataFlowPathWeight(succ);
    }
    if (implicitBooleanCondition
        && curr.stmt().getTarget().isAssign()
        && curr.stmt().getTarget().getLeftOp().getType().isBooleanType()) {
      return new DataFlowPathWeight(
          curr.stmt().getTarget().getLeftOp(),
          curr.stmt().getTarget().getRightOp().toString().equals("0")
              ? ConditionDomain.FALSE
              : ConditionDomain.TRUE);
    }

    if (implicitBooleanCondition && succ.stmt().getTarget().isReturnStmt()) {
      return new DataFlowPathWeight(succ.stmt().getTarget().getReturnOp());
    }

    if (trackPathConditions && curr.stmt().getTarget().isIfStmt()) {
      if (curr.stmt().getTarget().getIfStmt().getTarget().equals(succ.stmt())) {
        return new DataFlowPathWeight(curr.stmt().getTarget(), true);
      }
      return new DataFlowPathWeight(curr.stmt().getTarget(), false);
    }
    return DataFlowPathWeight.one();
  }

  @Override
  public DataFlowPathWeight pop(Node<Edge, Val> curr) {
    return DataFlowPathWeight.one();
  }

  @Override
  public DataFlowPathWeight getOne() {
    return DataFlowPathWeight.one();
  }
}
