package boomerang.shared.context;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.callgraph.ObservableICFG;
import boomerang.results.AbstractBoomerangResults.Context;
import boomerang.results.BackwardBoomerangResults;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Method;
import boomerang.scene.SootDataFlowScope;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.IntAndStringBoomerangOptions;
import boomerang.scene.jimple.SootCallGraph;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.text.CollationElementIterator;
import java.util.Collection;
import java.util.Collections;
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
  private Set<BackwardQuery> visited = Sets.newHashSet();

  public SharedContextAnalysis(){
    callGraph = new SootCallGraph();
    scope = SootDataFlowScope.make(Scene.v());
    customBoomerangOptions = new IntAndStringBoomerangOptions(){
      @Override
      public Optional<AllocVal> getAllocationVal(Method m, Statement stmt, Val fact,
          ObservableICFG<Statement, Method> icfg) {
        if (stmt.isAssign() && stmt.getLeftOp().equals(fact) && isStringOrIntAllocation(stmt)) {
          return Optional.of(new AllocVal(stmt.getLeftOp(), stmt, stmt.getRightOp()));
        }
        if(stmt.containsInvokeExpr()){

          if(isInSourceList(stmt)) {
            if (stmt.getInvokeExpr().getBase().equals(fact)) {
              return Optional.of(new AllocVal(stmt.getInvokeExpr().getBase(), stmt, stmt.getInvokeExpr().getBase()));
            }
          }
        }
        return super.getAllocationVal(m,stmt,fact, icfg);
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

  private boolean isStringOrIntAllocation(Statement stmt) {
    return stmt.isAssign() && (stmt.getRightOp().isIntConstant() || stmt.getRightOp().isStringConstant());
  }

  private boolean isInSourceList(Statement stmt) {
    return stmt.getInvokeExpr().getMethod().getDeclaringClass().getFullyQualifiedName().equals("java.lang.String") && stmt.getInvokeExpr().getMethod().getName().equals("<init>");
  }

  public Collection<ForwardQuery> run(BackwardQuery query) {
    queryQueue.add(new QueryWithContext(query));
    Boomerang bSolver = new Boomerang(callGraph, scope, customBoomerangOptions);
    Collection<ForwardQuery> finalAllocationSites = Sets.newHashSet();
    while(!queryQueue.isEmpty()){
      QueryWithContext pop = queryQueue.pop();
      BackwardBoomerangResults<NoWeight> results;
      if(pop.parentQuery == null) {
        results = bSolver.solve((BackwardQuery) pop.query);
      } else{
        results = bSolver.solveUnderScope((BackwardQuery) pop.query, pop.triggeringNode, (BackwardQuery) pop.parentQuery);
      }
      Map<ForwardQuery, Context> allocationSites = results.getAllocationSites();
      System.out.println(allocationSites);
      for(Entry<ForwardQuery, Context> entry : allocationSites.entrySet()){
        ForwardQuery forwardQuery = entry.getKey();
        Statement start = forwardQuery.cfgEdge().getStart();
        Method method = start.getMethod();
        System.out.println(start);
        if(start.containsInvokeExpr() && isInSourceList(start)){
          System.out.println("In source lists");
          for(Statement pred : method.getControlFlowGraph().getPredsOf(start)) {
            BackwardQuery newQuery = BackwardQuery.make(new Edge(pred, start), start.getInvokeExpr().getArg(0));
            if(visited.add(newQuery)){
              //TODO Same query not triggered under different context.
              queryQueue.add(new QueryWithContext(newQuery, new Node<>(forwardQuery.cfgEdge(), ((AllocVal) forwardQuery.var()).getDelegate()), pop.query));
            }
          }
        }
        if(isStringOrIntAllocation(start)){
          finalAllocationSites.add(entry.getKey());
        }
      }
    }
    

    return finalAllocationSites;
  }

  private static class QueryWithContext{
    private QueryWithContext(Query query){
      this.query = query;
    }

    private QueryWithContext(Query query, Node<Edge,Val> triggeringNode, Query parentQuery){
      this.query = query;
      this.parentQuery = parentQuery;
      this.triggeringNode = triggeringNode;
    }

    Query query;
    Query parentQuery;
    Node<Edge, Val> triggeringNode;
  }
}
