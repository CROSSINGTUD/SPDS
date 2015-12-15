package pathexpression;

import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;

public class PathExpressionComputer<N, V> {

  private LabeledGraph<N, V> graph;
  private BiMap<N, Integer> nodeToIntMap = HashBiMap.create();
  private Table<Integer, Integer, RegEx<V>> table = HashBasedTable.create();

  public PathExpressionComputer(LabeledGraph<N, V> graph) {
    this.graph = graph;
    initNodesToIntMap();
  }


  private void initNodesToIntMap() {
    for (N node : graph.getNodes()) {
      nodeToIntMap.put(node, (nodeToIntMap.size() + 1));
    }
    System.out.println(nodeToIntMap);
  }

  private Integer getIntegerFor(N node) {
    assert nodeToIntMap.get(node) != null;
    return nodeToIntMap.get(node);
  }

  private N getNodeFor(Integer i) {
    assert nodeToIntMap.inverse().get(i) != null;
    return nodeToIntMap.inverse().get(i);
  }

  public RegEx<V> getExpressionBetween(N a, N b) {
    eliminate();
    List<PathExpression<N, V>> allExpr = extractPathSequence();
    for (PathExpression<N, V> expr : allExpr) {
      if (expr.getSource().equals(a) && expr.getTarget().equals(b))
        return expr.getExpression();
    }
    return RegEx.<V>emptySet();
  }

  private List<PathExpression<N, V>> extractPathSequence() {
    int n = graph.getNodes().size();
    List<PathExpression<N, V>> list = new LinkedList<PathExpression<N, V>>();
    for (int u = 1; u <= n; u++) {
      for (int w = u; w <= n; w++) {
        RegEx<V> reg = table.get(u, w);
        if (!(reg.equals(RegEx.emptySet())) && !(reg.equals(RegEx.epsilon()))) {
          list.add(new PathExpression<N, V>(reg, getNodeFor(u), getNodeFor(w)));
        }
      }
    }
    for (int u = n; u > 0; u--) {
      for (int w = 1; w < u; w++) {
        RegEx<V> reg = table.get(u, w);
        if (!(reg.equals(RegEx.emptySet()))) {
          list.add(new PathExpression<N, V>(reg, getNodeFor(u), getNodeFor(w)));
        }
      }
    }
    System.out.println(list);
    return list;
  }

  private void eliminate() {
    int numberOfNodes = graph.getNodes().size();
    for (int i = 1; i <= numberOfNodes; i++) {
      for (int j = 1; j <= numberOfNodes; j++) {
        table.put(i, j, RegEx.<V>emptySet());
      }
    }
    for (Edge<N, V> e : graph.getEdges()) {
      table.put(getIntegerFor(e.getStart()), getIntegerFor(e.getTarget()),
          new RegEx.Plain<V>(e.getLabel()));
    }
    for (int v = 1; v <= numberOfNodes; v++) {
      RegEx<V> pvv = table.get(v, v);
      table.put(v, v, RegEx.<V>star(pvv));
      int u = v + 1;
      int w = v + 1;
      for (; u <= numberOfNodes; u++) {
        RegEx<V> puv = table.get(u, v);
        if (puv.equals(RegEx.emptySet())) {
          continue;
        }
        puv = RegEx.<V>concatenate(puv, pvv);
        table.put(u, v, puv);
        for (; w <= numberOfNodes; w++) {
          RegEx<V> pvw = table.get(v, w);
          if (pvw.equals(RegEx.emptySet())) {
            continue;
          }

          RegEx<V> old_puw = table.get(u, w);
          RegEx<V> a = RegEx.<V>concatenate(puv, pvw);
          RegEx<V> puw = RegEx.<V>union(old_puw, a);
          table.put(u, w, puw);
        }
      }
    }
    System.out.println(table);
  }
}
