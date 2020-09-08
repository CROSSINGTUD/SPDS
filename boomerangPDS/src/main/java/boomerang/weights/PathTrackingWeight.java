package boomerang.weights;

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Val;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public class PathTrackingWeight extends Weight {

  private static PathTrackingWeight one;
  /**
   * This set keeps track of all statements on a shortest path that use an alias from source to
   * sink.
   */
  private LinkedHashSet<Node<Edge, Val>> shortestPathWitness = new LinkedHashSet<>();
  /**
   * This set keeps track of all statement along all paths that use an alias from source to sink.
   */
  private Set<LinkedHashSet<Node<Edge, Val>>> allPathWitness = Sets.newHashSet();

  private String rep;

  private PathTrackingWeight(String rep) {
    this.rep = rep;
  }

  private PathTrackingWeight(
      LinkedHashSet<Node<Edge, Val>> allStatement,
      Set<LinkedHashSet<Node<Edge, Val>>> allPathWitness) {
    this.shortestPathWitness = allStatement;
    this.allPathWitness = allPathWitness;
  }

  public PathTrackingWeight(Node<Edge, Val> relevantStatement) {
    this.shortestPathWitness.add(relevantStatement);
    LinkedHashSet<Node<Edge, Val>> firstDataFlowPath = new LinkedHashSet<>();
    firstDataFlowPath.add(relevantStatement);
    this.allPathWitness.add(firstDataFlowPath);
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
    LinkedHashSet<Node<Edge, Val>> newAllStatements = new LinkedHashSet<>();
    newAllStatements.addAll(shortestPathWitness);
    newAllStatements.addAll(other.shortestPathWitness);

    Set<LinkedHashSet<Node<Edge, Val>>> newAllPathStatements = new LinkedHashSet<>();
    for (LinkedHashSet<Node<Edge, Val>> pathPrefix : allPathWitness) {
      for (LinkedHashSet<Node<Edge, Val>> pathSuffix : other.allPathWitness) {
        LinkedHashSet<Node<Edge, Val>> combinedPath = Sets.newLinkedHashSet();
        combinedPath.addAll(pathPrefix);
        combinedPath.addAll(pathSuffix);
        newAllPathStatements.add(combinedPath);
      }
    }
    if (allPathWitness.isEmpty()) {
      for (LinkedHashSet<Node<Edge, Val>> pathSuffix : other.allPathWitness) {
        LinkedHashSet<Node<Edge, Val>> combinedPath = Sets.newLinkedHashSet();
        combinedPath.addAll(pathSuffix);
        newAllPathStatements.add(combinedPath);
      }
    }
    if (other.allPathWitness.isEmpty()) {
      for (LinkedHashSet<Node<Edge, Val>> pathSuffix : allPathWitness) {
        LinkedHashSet<Node<Edge, Val>> combinedPath = Sets.newLinkedHashSet();
        combinedPath.addAll(pathSuffix);
        newAllPathStatements.add(combinedPath);
      }
    }

    return new PathTrackingWeight(newAllStatements, newAllPathStatements);
  }

  @Override
  public Weight combineWith(Weight o) {
    if (!(o instanceof PathTrackingWeight))
      throw new RuntimeException("Cannot extend to different types of weight!");
    PathTrackingWeight other = (PathTrackingWeight) o;
    Set<LinkedHashSet<Node<Edge, Val>>> newAllPathStatements = new LinkedHashSet<>();
    for (LinkedHashSet<Node<Edge, Val>> pathPrefix : allPathWitness) {
      LinkedHashSet<Node<Edge, Val>> combinedPath = Sets.newLinkedHashSet();
      combinedPath.addAll(pathPrefix);
      newAllPathStatements.add(combinedPath);
    }
    for (LinkedHashSet<Node<Edge, Val>> pathPrefix : other.allPathWitness) {
      LinkedHashSet<Node<Edge, Val>> combinedPath = Sets.newLinkedHashSet();
      combinedPath.addAll(pathPrefix);
      newAllPathStatements.add(combinedPath);
    }

    if (shortestPathWitness.size() > other.shortestPathWitness.size()) {
      return new PathTrackingWeight(
          new LinkedHashSet<>(other.shortestPathWitness), newAllPathStatements);
    }

    return new PathTrackingWeight(
        new LinkedHashSet<>(this.shortestPathWitness), newAllPathStatements);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((shortestPathWitness == null) ? 0 : shortestPathWitness.hashCode());
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PathTrackingWeight other = (PathTrackingWeight) obj;
    if (shortestPathWitness == null) {
      if (other.shortestPathWitness != null) return false;
    } else if (!shortestPathWitness.equals(other.shortestPathWitness)) return false;
    if (allPathWitness == null) {
      if (other.allPathWitness != null) return false;
    } else if (!allPathWitness.equals(other.allPathWitness)) return false;
    if (rep == null) {
      if (other.rep != null) return false;
    } else if (!rep.equals(other.rep)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "\nAll statements: " + shortestPathWitness;
  }

  public List<Node<Edge, Val>> getShortestPathWitness() {
    return Lists.newArrayList(shortestPathWitness);
  }

  public Set<LinkedHashSet<Node<Edge, Val>>> getAllPathWitness() {
    return Sets.newHashSet(allPathWitness);
  }
}
