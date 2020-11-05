package boomerang.shared.context;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.QueryGraph;
import boomerang.callgraph.ObservableICFG;
import boomerang.results.AbstractBoomerangResults.Context;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Method;
import boomerang.scene.SootDataFlowScope;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.IntAndStringBoomerangOptions;
import boomerang.scene.jimple.SootCallGraph;
import boomerang.solver.ForwardBoomerangSolver;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import soot.Scene;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight.NoWeight;

public class SharedContextAnalysis {

  private final DefaultBoomerangOptions customBoomerangOptions;
  private DataFlowScope scope;
  private SootCallGraph callGraph;
  private LinkedList<QueryWithContext> queryQueue = Lists.newLinkedList();
  private Set<Query> visited = Sets.newHashSet();

  public SharedContextAnalysis() {
    callGraph = new SootCallGraph();
    scope = SootDataFlowScope.make(Scene.v());
    customBoomerangOptions =
        new IntAndStringBoomerangOptions() {
          @Override
          public Optional<AllocVal> getAllocationVal(
              Method m, Statement stmt, Val fact, ObservableICFG<Statement, Method> icfg) {

            if (stmt.isAssign() && stmt.getLeftOp().equals(fact) && isStringOrIntAllocation(stmt)) {
              return Optional.of(new AllocVal(stmt.getLeftOp(), stmt, stmt.getRightOp()));
            }
            if (stmt.containsInvokeExpr()) {
              if (isInSourceList(stmt)) {
                if (stmt.getInvokeExpr().getBase().equals(fact)) {
                  return Optional.of(
                      new AllocVal(
                          stmt.getInvokeExpr().getBase(), stmt, stmt.getInvokeExpr().getBase()));
                }
              }
            }
            return super.getAllocationVal(m, stmt, fact, icfg);
          }

          @Override
          public int analysisTimeoutMS() {
            return 1999999;
          }

          @Override
          public boolean allowMultipleQueries() {
            return true;
          }
        };
  }

  public Collection<ForwardQuery> run(Query query) {
    queryQueue.add(new QueryWithContext(query));
    Boomerang bSolver = new Boomerang(callGraph, scope, customBoomerangOptions);
    Collection<ForwardQuery> finalAllocationSites = Sets.newHashSet();
    while (!queryQueue.isEmpty()) {
      QueryWithContext pop = queryQueue.pop();
      if (pop.query instanceof ForwardQuery) {
        ForwardBoomerangResults<NoWeight> results;
        ForwardQuery currentQuery = (ForwardQuery) pop.query;
        if (pop.parentQuery == null) {
          results = bSolver.solve(currentQuery);
        } else {
          results = bSolver.solveUnderScope(currentQuery, pop.triggeringNode, pop.parentQuery);
        }

        Table<Edge, Val, NoWeight> forwardResults =
            results.asStatementValWeightTable((ForwardQuery) pop.query);
        // Any ForwardQuery may trigger additional ForwardQuery under its own scope.
        triggerNewForwardQueries(forwardResults, currentQuery);
      } else {
        BackwardBoomerangResults<NoWeight> results;
        if (pop.parentQuery == null) {
          results = bSolver.solve((BackwardQuery) pop.query);
        } else {
          results =
              bSolver.solveUnderScope(
                  (BackwardQuery) pop.query, pop.triggeringNode, pop.parentQuery);
        }
        Map<ForwardQuery, Context> allocationSites = results.getAllocationSites();

        System.out.println(pop.query);
        System.out.println(allocationSites);
        for (Entry<ForwardQuery, Context> entry : allocationSites.entrySet()) {
          ForwardQuery forwardQuery = entry.getKey();
          Statement start = forwardQuery.cfgEdge().getStart();
          Method method = start.getMethod();
          System.out.println(start);
          if (start.containsInvokeExpr() && isInSourceList(start)) {
            System.out.println("In source lists");
            for (Statement pred : method.getControlFlowGraph().getPredsOf(start)) {
              BackwardQuery newQuery =
                  BackwardQuery.make(new Edge(pred, start), start.getInvokeExpr().getArg(0));

              // Backward -> AllocSite -> new BackwardQuery
              addToQueue(
                  new QueryWithContext(
                      newQuery,
                      new Node<>(
                          forwardQuery.cfgEdge(), ((AllocVal) forwardQuery.var()).getDelegate()),
                      pop.query));
            }
          }
          if (isStringOrIntAllocation(start)) {
            finalAllocationSites.add(entry.getKey());
          }
        }
        // Any ForwardQuery may trigger additional ForwardQuery under its own scope.
        for (ForwardBoomerangSolver<NoWeight> solver : bSolver.getSolvers().values()) {
          triggerNewForwardQueries(solver.asStatementValWeightTable(), solver.getQuery());
        }
      }
    }

    QueryGraph<NoWeight> queryGraph = bSolver.getQueryGraph();
    System.out.println(queryGraph.toDotString());
    return finalAllocationSites;
  }

  private void triggerNewForwardQueries(
      Table<Edge, Val, NoWeight> forwardResults, Query currentQuery) {
    for (Cell<Edge, Val, NoWeight> cell : forwardResults.cellSet()) {
      Val reachingVariable = cell.getColumnKey();
      Edge edge = cell.getRowKey();
      Collection<Query> newQueries = createNextQuery(edge, reachingVariable);
      for (Query newQuery : newQueries) {
        addToQueue(
            new QueryWithContext(newQuery, new Node<>(edge, reachingVariable), currentQuery));
      }
    }
  }

  private void addToQueue(QueryWithContext nextQuery) {
    if (visited.add(nextQuery.query)) {
      queryQueue.add(nextQuery);
    }
  }

  private boolean isStringOrIntAllocation(Statement stmt) {
    return stmt.isAssign()
        && (stmt.getRightOp().isIntConstant() || stmt.getRightOp().isStringConstant());
  }

  private boolean isInSourceList(Statement stmt) {
    return stmt.getInvokeExpr()
            .getMethod()
            .getDeclaringClass()
            .getFullyQualifiedName()
            .equals("java.lang.String")
        && stmt.getInvokeExpr().getMethod().getName().equals("<init>");
  }

  private Collection<Query> createNextQuery(Edge edge, Val reachingVariable) {
    Statement potentialCallSite = edge.getTarget();
    Method method = potentialCallSite.getMethod();
    if (potentialCallSite.containsInvokeExpr() && isInSourceList(potentialCallSite)) {
      if (potentialCallSite.getInvokeExpr().getArgs().contains(reachingVariable)) {
        Set<Query> res = Sets.newHashSet();
        for (Statement succ : method.getControlFlowGraph().getSuccsOf(potentialCallSite)) {
          Val base = potentialCallSite.getInvokeExpr().getBase();
          res.add(
              new ForwardQuery(
                  new Edge(potentialCallSite, succ), new AllocVal(base, potentialCallSite, base)));
        }

        return res;
      }
    }
    return Sets.newHashSet();
  }

  private boolean isInNextQueryList(Statement potentialCallSite, Val reachingVariable) {
    if (isInSourceList(potentialCallSite)) {
      if (potentialCallSite.getInvokeExpr().getArgs().contains(reachingVariable)) {
        return true;
      }
    }
    return false;
  }

  private static class QueryWithContext {
    private QueryWithContext(Query query) {
      this.query = query;
    }

    private QueryWithContext(Query query, Node<Edge, Val> triggeringNode, Query parentQuery) {
      this.query = query;
      this.parentQuery = parentQuery;
      this.triggeringNode = triggeringNode;
    }

    Query query;
    Query parentQuery;
    Node<Edge, Val> triggeringNode;
  }
}
