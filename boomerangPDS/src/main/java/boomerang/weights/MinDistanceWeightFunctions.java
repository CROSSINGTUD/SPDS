package boomerang.weights;

import boomerang.scene.Statement;
import boomerang.scene.Val;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;

public class MinDistanceWeightFunctions
    implements WeightFunctions<Statement, Val, Statement, MinDistanceWeight> {

  @Override
  public MinDistanceWeight push(
      Node<Statement, Val> curr, Node<Statement, Val> succ, Statement callSite) {
    if (!curr.fact().isStatic()) {
      return new MinDistanceWeight(new Integer(1));
    }
    return MinDistanceWeight.one();
  }

  @Override
  public MinDistanceWeight normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
    if (!curr.fact().equals(succ.fact())) {
      return new MinDistanceWeight(new Integer(1));
    }
    if (succ.stmt().containsInvokeExpr() && succ.stmt().uses(curr.fact())) {
      return new MinDistanceWeight(new Integer(1));
    }
    return MinDistanceWeight.one();
  }

  @Override
  public MinDistanceWeight pop(Node<Statement, Val> curr) {
    return MinDistanceWeight.one();
  }

  @Override
  public MinDistanceWeight getOne() {
    return MinDistanceWeight.one();
  }
}
