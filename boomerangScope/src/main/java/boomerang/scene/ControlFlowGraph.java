package boomerang.scene;

import java.util.Collection;
import java.util.List;

public interface ControlFlowGraph {

  public Collection<Statement> getStartPoints();

  public Collection<Statement> getEndPoints();

  public Collection<Statement> getSuccsOf(Statement curr);

  public Collection<Statement> getPredsOf(Statement curr);

  public List<Statement> getStatements();
}
