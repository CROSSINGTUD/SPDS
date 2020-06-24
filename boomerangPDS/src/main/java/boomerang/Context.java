package boomerang;

import boomerang.scene.Statement;

/**
 * A context is stored within the context graph. And must at least have a statement associated with
 * it. This will be used by the {@link ContextRequester} to retrieve more contexts upon need.
 *
 * @author "Johannes Spaeth"
 */
public interface Context {
  public Statement getStmt();

  public int hashCode();

  public boolean equals(Object obj);
}
