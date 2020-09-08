package boomerang.scene;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;

public class CallGraph {

  private Set<Edge> edges = Sets.newHashSet();
  private Multimap<Statement, Edge> edgesOutOf = HashMultimap.create();
  private Multimap<Method, Edge> edgesInto = HashMultimap.create();
  private Set<Method> entryPoints = Sets.newHashSet();
  private Multimap<Field, Statement> fieldLoadStatements = HashMultimap.create();
  private Multimap<Field, Statement> fieldStoreStatements = HashMultimap.create();

  public Collection<Edge> edgesOutOf(Statement stmt) {
    return edgesOutOf.get(stmt);
  }

  public static class Edge {

    private final Statement callSite;
    private final Method callee;

    public Edge(Statement callSite, Method callee) {
      assert callSite.containsInvokeExpr();
      this.callSite = callSite;
      this.callee = callee;
    }

    public Method tgt() {
      return callee;
    }

    public Statement src() {
      return callSite;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
      result = prime * result + ((callee == null) ? 0 : callee.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Edge other = (Edge) obj;
      if (callSite == null) {
        if (other.callSite != null) return false;
      } else if (!callSite.equals(other.callSite)) return false;
      if (callee == null) {
        if (other.callee != null) return false;
      } else if (!callee.equals(other.callee)) return false;
      return true;
    }

    @Override
    public String toString() {
      return "Call Graph Edge: " + callSite + " calls " + tgt();
    }
  }

  public boolean addEdge(Edge edge) {
    edgesOutOf.put(edge.callSite, edge);
    edgesInto.put(edge.tgt(), edge);
    computeStaticFieldsLoadAndStores(edge.tgt());
    return edges.add(edge);
  }

  public Collection<Edge> edgesInto(Method m) {
    return edgesInto.get(m);
  }

  public int size() {
    return edges.size();
  }

  public Set<Edge> getEdges() {
    return edges;
  }

  public Collection<Method> getEntryPoints() {
    return entryPoints;
  }

  public boolean addEntryPoint(Method m) {
    computeStaticFieldsLoadAndStores(m);
    return entryPoints.add(m);
  }

  public Set<Method> getReachableMethods() {
    Set<Method> reachableMethod = Sets.newHashSet();
    reachableMethod.addAll(entryPoints);
    reachableMethod.addAll(edgesInto.keySet());
    return reachableMethod;
  }

  public Multimap<Field, Statement> getFieldStoreStatements() {
    return fieldStoreStatements;
  }

  public Multimap<Field, Statement> getFieldLoadStatements() {
    return fieldLoadStatements;
  }

  private void computeStaticFieldsLoadAndStores(Method m) {
    for (Statement s : m.getStatements()) {
      if (s.isStaticFieldStore()) {
        fieldStoreStatements.put(s.getStaticField().field(), s);
      }
      if (s.isStaticFieldLoad()) {
        fieldLoadStatements.put(s.getStaticField().field(), s);
      }
    }
  }
}
