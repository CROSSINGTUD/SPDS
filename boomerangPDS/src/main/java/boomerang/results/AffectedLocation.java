package boomerang.results;

import boomerang.scene.Statement;
import boomerang.scene.Val;
import java.util.List;

public interface AffectedLocation {

  Statement getStatement();

  Val getVariable();

  List<PathElement> getDataFlowPath();

  String getMessage();

  int getRuleIndex();
}
