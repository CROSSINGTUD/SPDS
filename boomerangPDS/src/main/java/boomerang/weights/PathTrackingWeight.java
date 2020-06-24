package boomerang.weights;

import boomerang.scene.Statement;
import boomerang.scene.Val;
import com.google.common.collect.Lists;
import java.util.LinkedHashSet;
import java.util.List;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public class PathTrackingWeight extends Weight {

  private static PathTrackingWeight one;
  /** This set keeps track of all statement that use an alias from source to sink. */
  private LinkedHashSet<Node<Statement, Val>> allStatements = new LinkedHashSet<>();

  private String rep;

  private PathTrackingWeight(String rep) {
    this.rep = rep;
  }

  private PathTrackingWeight(LinkedHashSet<Node<Statement, Val>> allStatement) {
    this.allStatements = allStatement;
  }

  public PathTrackingWeight(Node<Statement, Val> relevantStatement) {
    allStatements.add(relevantStatement);
  }

  public static PathTrackingWeight one() {
    if (one == null) {
      one = new PathTrackingWeight("ONE");
    }
    return one;
  }

  @Override
  public Weight extendWith(Weight o) {
    if (!(o instanceof PathTrackingWeight))
      throw new RuntimeException("Cannot extend to different types of weight!");
    PathTrackingWeight other = (PathTrackingWeight) o;
    LinkedHashSet<Node<Statement, Val>> newAllStatements = new LinkedHashSet<>();
    newAllStatements.addAll(allStatements);
    newAllStatements.addAll(other.allStatements);
    return new PathTrackingWeight(newAllStatements);
  }

  @Override
  public Weight combineWith(Weight o) {
    if (!(o instanceof PathTrackingWeight))
      throw new RuntimeException("Cannot extend to different types of weight!");
    PathTrackingWeight other = (PathTrackingWeight) o;
    if (allStatements.size() > other.allStatements.size()) {
      return new PathTrackingWeight(new LinkedHashSet<>(other.allStatements));
    }
    return new PathTrackingWeight(new LinkedHashSet<>(this.allStatements));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((allStatements == null) ? 0 : allStatements.hashCode());
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PathTrackingWeight other = (PathTrackingWeight) obj;
    if (allStatements == null) {
      if (other.allStatements != null) return false;
    } else if (!allStatements.equals(other.allStatements)) return false;
    if (rep == null) {
      if (other.rep != null) return false;
    } else if (!rep.equals(other.rep)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "\nAll statements: " + allStatements;
  }

  public List<Node<Statement, Val>> getAllStatements() {
    return Lists.newArrayList(allStatements);
  }
}
