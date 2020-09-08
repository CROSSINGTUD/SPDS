package test.core;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import java.util.Optional;

class IntegerAllocationSiteOf implements ValueOfInterestInUnit {
  public Optional<? extends Query> test(Edge cfgEdge) {
    Statement stmt = cfgEdge.getStart();
    if (stmt.isAssign()) {
      if (stmt.getLeftOp().toString().contains("allocation")) {
        if (stmt.getLeftOp().isLocal() && stmt.getRightOp().isIntConstant()) {
          Val local = stmt.getLeftOp();
          ForwardQuery forwardQuery = new ForwardQuery(cfgEdge, local);
          return Optional.of(forwardQuery);
        }
      }
    }

    return Optional.empty();
  }
}
