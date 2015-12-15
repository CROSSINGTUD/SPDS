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
  }

  private Integer getIntegerFor(N node) {
    assert nodeToIntMap.get(node) != null;
    return nodeToIntMap.get(node);
  }

  public RegEx<V> getExpressionBetween(N a, N b) {
    if (!graph.getNodes().contains(a))
      return RegEx.<V>emptySet();
    eliminate();
    List<RegEx<V>> allExpr = computeAllPathFrom(a);
    return allExpr.get(getIntegerFor(b) - 1);
  }

  private List<RegEx<V>> computeAllPathFrom(N a) {
    assert graph.getNodes().contains(a);
    eliminate();
    List<PathExpression<V>> extractPathSequence = extractPathSequence();
    List<RegEx<V>> regEx = new LinkedList<>();
    for (int i = 0; i < graph.getNodes().size(); i++)
      regEx.add(RegEx.<V>emptySet());
    regEx.set(getIntegerFor(a) - 1, RegEx.<V>epsilon());
    for (int i = 0; i < extractPathSequence.size(); i++) {
      PathExpression<V> tri = extractPathSequence.get(i);
      if (tri.getSource() == tri.getTarget()) {
        RegEx<V> expression = tri.getExpression();

        int vi = tri.getSource();
        RegEx<V> regExVi = regEx.get(vi - 1);
        regEx.set(vi - 1, RegEx.<V>concatenate(regExVi, expression));

      } else {
        RegEx<V> expression = tri.getExpression();
        int vi = tri.getSource();
        int wi = tri.getTarget();
        RegEx<V> inter;
        RegEx<V> regExVi = regEx.get(vi - 1);
        inter = RegEx.simplify(RegEx.<V>concatenate(regExVi, expression));

        RegEx<V> regExWi = regEx.get(wi - 1);
        regEx.set(wi - 1, RegEx.simplify(RegEx.<V>union(RegEx.<V>simplify(regExWi), inter)));
      }
    }
    return regEx;
  }

  private List<PathExpression<V>> extractPathSequence() {
    int n = graph.getNodes().size();
    List<PathExpression<V>> list = new LinkedList<PathExpression<V>>();
    for (int u = 1; u <= n; u++) {
      for (int w = u; w <= n; w++) {
        RegEx<V> reg = table.get(u, w);
        if (!(reg.equals(RegEx.emptySet())) && !(reg.equals(RegEx.epsilon()))) {
          list.add(new PathExpression<V>(reg, u, w));
        }
      }
    }
    for (int u = n; u > 0; u--) {
      for (int w = 1; w < u; w++) {
        RegEx<V> reg = table.get(u, w);
        if (!(reg.equals(RegEx.emptySet()))) {
          list.add(new PathExpression<V>(reg, u, w));
        }
      }
    }
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
      Integer head = getIntegerFor(e.getStart());
      Integer tail = getIntegerFor(e.getTarget());
      RegEx<V> pht = table.get(head, tail);
      pht = RegEx.<V>union(new RegEx.Plain<V>(e.getLabel()), pht);
      table.put(head, tail, pht);
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
  }
}
