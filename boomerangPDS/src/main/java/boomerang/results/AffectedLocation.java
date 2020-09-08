package boomerang.results;

import boomerang.scene.ControlFlowGraph;
import boomerang.scene.Val;
import java.util.List;

public interface AffectedLocation {

  ControlFlowGraph.Edge getStatement();

  Val getVariable();

  List<PathElement> getDataFlowPath();

  String getMessage();

  int getRuleIndex();
}
