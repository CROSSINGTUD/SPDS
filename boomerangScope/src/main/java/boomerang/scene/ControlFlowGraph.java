package boomerang.scene;

import java.util.Collection;
import java.util.List;
import wpds.interfaces.Location;

public interface ControlFlowGraph {

  Collection<Statement> getStartPoints();

  Collection<Statement> getEndPoints();

  Collection<Statement> getSuccsOf(Statement curr);

  Collection<Statement> getPredsOf(Statement curr);

  List<Statement> getStatements();

  class Edge extends Pair<Statement, Statement> implements Location {
    public Edge(Statement start, Statement target) {
      super(start, target);
      if (!start.equals(Statement.epsilon()) && !start.getMethod().equals(target.getMethod())) {
        throw new RuntimeException("Illegal Control Flow Graph Edge constructed");
      }
    }

    @Override
    public String toString() {
      return getStart() + " -> " + getTarget();
    }

    public Statement getStart() {
      return getX();
    }

    public Statement getTarget() {
      return getY();
    }

    public Method getMethod() {
      return getStart().getMethod();
    }

    @Override
    public boolean accepts(Location other) {
      return this.equals(other);
    }
  }
}
