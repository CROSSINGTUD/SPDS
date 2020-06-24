package boomerang.results;

import boomerang.Query;
import boomerang.scene.Method;
import java.util.Collection;
import java.util.Set;

public class QueryResults {
  private Query query;
  private final Collection<Method> visitedMethods;
  private final Collection<AffectedLocation> affectedLocations;
  private final boolean timedout;

  public QueryResults(
      Query query, Set<AffectedLocation> npes, Set<Method> visMethod, boolean timedout) {
    this.query = query;
    this.visitedMethods = visMethod;
    this.affectedLocations = npes;
    this.timedout = timedout;
  }

  public Query getQuery() {
    return query;
  }

  public Collection<Method> getVisitedMethods() {
    return visitedMethods;
  }

  public Collection<AffectedLocation> getAffectedLocations() {
    return affectedLocations;
  }

  public boolean isTimedout() {
    return timedout;
  }
}
