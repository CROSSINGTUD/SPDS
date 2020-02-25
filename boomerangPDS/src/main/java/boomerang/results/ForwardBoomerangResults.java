package boomerang.results;

import boomerang.ForwardQuery;
import boomerang.Util;
import boomerang.callgraph.CallerListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.controlflowgraph.ObservableControlFlowGraph;
import boomerang.controlflowgraph.PredecessorListener;
import boomerang.scene.DeclaredMethod;
import boomerang.scene.Field;
import boomerang.scene.IfStatement;
import boomerang.scene.IfStatement.Evaluation;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.JimpleVal;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import boomerang.stats.IBoomerangStats;
import boomerang.util.DefaultValueMap;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.*;
import java.util.Map.Entry;
import soot.jimple.IntConstant;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;

public class ForwardBoomerangResults<W extends Weight> extends AbstractBoomerangResults<W> {

  private final ForwardQuery query;
  private final boolean timedout;
  private final IBoomerangStats<W> stats;
  private Stopwatch analysisWatch;
  private long maxMemory;
  private ObservableICFG<Statement, Method> icfg;
  private Set<Method> visitedMethods;
  private ObservableControlFlowGraph cfg;

  public ForwardBoomerangResults(
      ForwardQuery query,
      ObservableICFG<Statement, Method> icfg,
      ObservableControlFlowGraph cfg,
      boolean timedout,
      DefaultValueMap<ForwardQuery, ForwardBoomerangSolver<W>> queryToSolvers,
      IBoomerangStats<W> stats,
      Stopwatch analysisWatch,
      Set<Method> visitedMethods) {
    super(queryToSolvers);
    this.query = query;
    this.icfg = icfg;
    this.cfg = cfg;
    this.timedout = timedout;
    this.stats = stats;
    this.analysisWatch = analysisWatch;
    this.visitedMethods = visitedMethods;
    stats.terminated(query, this);
    this.maxMemory = Util.getReallyUsedMemory();
  }

  public Stopwatch getAnalysisWatch() {
    return analysisWatch;
  }

  public boolean isTimedout() {
    return timedout;
  }

  public Table<Statement, Val, W> getObjectDestructingStatements() {
    AbstractBoomerangSolver<W> solver = queryToSolvers.get(query);
    if (solver == null) {
      return HashBasedTable.create();
    }
    Table<Statement, Val, W> res = asStatementValWeightTable();
    Set<Method> visitedMethods = Sets.newHashSet();
    for (Statement s : res.rowKeySet()) {
      visitedMethods.add(s.getMethod());
    }
    ForwardBoomerangSolver<W> forwardSolver = (ForwardBoomerangSolver) queryToSolvers.get(query);
    Table<Statement, Val, W> destructingStatement = HashBasedTable.create();
    for (Method flowReaches : visitedMethods) {
      for (Statement exitStmt : icfg.getEndPointsOf(flowReaches)) {
        Set<State> escapes = Sets.newHashSet();
        icfg.addCallerListener(
            new CallerListener<Statement, Method>() {
              @Override
              public Method getObservedCallee() {
                return flowReaches;
              }

              @Override
              public void onCallerAdded(Statement callSite, Method m) {
                Method callee = callSite.getMethod();
                if (visitedMethods.contains(callee)) {
                  for (Entry<Val, W> valAndW : res.row(exitStmt).entrySet()) {
                    escapes.addAll(
                        forwardSolver.computeReturnFlow(flowReaches, exitStmt, valAndW.getKey()));
                  }
                }
              }
            });
        if (escapes.isEmpty()) {
          Map<Val, W> row = res.row(exitStmt);
          findLastUsage(exitStmt, row, destructingStatement, forwardSolver);
        }
      }
    }

    return destructingStatement;
  }

  public Table<Statement, Val, W> asStatementValWeightTable() {
    return asStatementValWeightTable(query);
  }

  private void findLastUsage(
      Statement exitStmt,
      Map<Val, W> row,
      Table<Statement, Val, W> destructingStatement,
      ForwardBoomerangSolver<W> forwardSolver) {
    LinkedList<Statement> worklist = Lists.newLinkedList();
    worklist.add(exitStmt);
    Set<Statement> visited = Sets.newHashSet();
    while (!worklist.isEmpty()) {
      Statement curr = worklist.poll();
      if (!visited.add(curr)) {
        continue;
      }
      boolean valueUsedInStmt = false;
      for (Entry<Val, W> e : row.entrySet()) {
        if (curr.valueUsedInStatement(e.getKey())) {
          destructingStatement.put(curr, e.getKey(), e.getValue());
          valueUsedInStmt = true;
        }
      }
      if (!valueUsedInStmt
          &&
          /** Do not continue over CatchStmt */
          !(curr.isIdentityStmt())) {
        cfg.addPredsOfListener(
            new PredecessorListener(curr) {

              @Override
              public void getPredecessor(Statement succ) {
                worklist.add(succ);
              }
            });
      }
    }
  }

  public IBoomerangStats<W> getStats() {
    return stats;
  }

  public Map<Statement, DeclaredMethod> getInvokedMethodOnInstance() {
    Map<Statement, DeclaredMethod> invokedMethodsOnInstance = Maps.newHashMap();
    if (query.stmt().containsInvokeExpr()) {
      invokedMethodsOnInstance.put(query.stmt(), query.stmt().getInvokeExpr().getMethod());
    }
    queryToSolvers
        .get(query)
        .getFieldAutomaton()
        .registerListener(
            new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {

              @Override
              public void onWeightAdded(
                  Transition<Field, INode<Node<Statement, Val>>> t,
                  W w,
                  WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
                if (!t.getLabel().equals(Field.empty()) || t.getStart() instanceof GeneratedState) {
                  return;
                }
                Node<Statement, Val> node = t.getStart().fact();
                Val fact = node.fact();
                Statement curr = node.stmt();
                if (curr.containsInvokeExpr()) {
                  if (curr.getInvokeExpr().isInstanceInvokeExpr()) {
                    Val base = curr.getInvokeExpr().getBase();
                    if (base.equals(fact)) {
                      invokedMethodsOnInstance.put(curr, curr.getInvokeExpr().getMethod());
                    }
                  }
                }
              }
            });
    return invokedMethodsOnInstance;
  }

  public QueryResults getPotentialNullPointerDereferences() {
    // FIXME this should be located nullpointer analysis
    Set<Node<Statement, Val>> res = Sets.newHashSet();
    for (Transition<Field, INode<Node<Statement, Val>>> t :
        queryToSolvers.get(query).getFieldAutomaton().getTransitions()) {
      if (!t.getLabel().equals(Field.empty()) || t.getStart() instanceof GeneratedState) {
        continue;
      }
      Node<Statement, Val> nullPointerNode = t.getStart().fact();
      if (NullPointerDereference.isNullPointerNode(nullPointerNode)
          && queryToSolvers.get(query).getReachedStates().contains(nullPointerNode)) {
        res.add(nullPointerNode);
      }
    }
    Set<NullPointerDereference> resWithContext = Sets.newHashSet();
    for (Node<Statement, Val> r : res) {
      resWithContext.add(
          new NullPointerDereference(query, r.stmt(), r.fact(), null, null));
    }
    QueryResults nullPointerResult =
        new QueryResults(query, resWithContext, visitedMethods, timedout);
    return nullPointerResult;
  }

  public Context getContext(Node<Statement, Val> node) {
    return constructContextGraph(query, node);
  }

  public boolean containsCallRecursion() {
    for (Entry<ForwardQuery, ForwardBoomerangSolver<W>> e : queryToSolvers.entrySet()) {
      if (e.getValue().getCallAutomaton().containsLoop()) {
        return true;
      }
    }
    return false;
  }

  public boolean containsFieldLoop() {
    for (Entry<ForwardQuery, ForwardBoomerangSolver<W>> e : queryToSolvers.entrySet()) {
      if (e.getValue().getFieldAutomaton().containsLoop()) {
        return true;
      }
    }
    return false;
  }

  public Set<Method> getVisitedMethods() {
    return visitedMethods;
  }

  public long getMaxMemory() {
    return maxMemory;
  }
}
