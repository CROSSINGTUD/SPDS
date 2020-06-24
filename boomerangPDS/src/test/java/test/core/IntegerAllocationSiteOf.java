package test.core;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import java.util.Optional;

class IntegerAllocationSiteOf implements ValueOfInterestInUnit {
  public Optional<? extends Query> test(Statement stmt) {
    if (stmt.isAssign()) {
      if (stmt.getLeftOp().toString().contains("allocation")) {
        if (stmt.getLeftOp().isLocal() && stmt.getRightOp().isIntConstant()) {
          Val local = stmt.getLeftOp();
          ForwardQuery forwardQuery = new ForwardQuery(stmt, local);
          return Optional.<Query>of(forwardQuery);
        }
      }
    }

    return Optional.empty();
  }
}
