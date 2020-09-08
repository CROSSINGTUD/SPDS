package test.core;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Statement;
import boomerang.scene.Type;
import boomerang.scene.Val;
import java.util.Optional;

class AllocationSiteOf implements ValueOfInterestInUnit {
  private String type;

  public AllocationSiteOf(String type) {
    this.type = type;
  }

  public Optional<Query> test(Edge cfgEdge) {
    Statement stmt = cfgEdge.getStart();
    if (stmt.isAssign()) {
      if (stmt.getLeftOp().isLocal() && stmt.getRightOp().isNewExpr()) {
        Type expr = stmt.getRightOp().getNewExprType();
        if (expr.isSubtypeOf(type)) {
          Val local = stmt.getLeftOp();
          ForwardQuery forwardQuery =
              new ForwardQuery(cfgEdge, new AllocVal(local, stmt, stmt.getRightOp()));
          return Optional.<Query>of(forwardQuery);
        }
      }
    }
    return Optional.empty();
  }
}
