package test;
import java.util.Set;

import com.google.common.collect.Sets;

import pathexpression.Edge;
import pathexpression.LabeledGraph;


public class IntGraph implements LabeledGraph<Integer, String> {

  private Set<Edge<Integer, String>> edges = Sets.newHashSet();
  private Set<Integer> nodes = Sets.newHashSet();
  private String eps = "EPS";

  public void addEdge(int start, String label, int target) {
    nodes.add(start);
    nodes.add(target);
    edges.add(new IntEdge(start, label, target));
  }

  @Override
  public Set<Edge<Integer, String>> getEdges() {
    return edges;
  }

  @Override
  public Set<Integer> getNodes() {
    return nodes;
  }

  @Override
  public String epsilon() {
    return eps;
  }

}
