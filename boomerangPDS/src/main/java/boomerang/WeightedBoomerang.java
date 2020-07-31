/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang;

import boomerang.BoomerangOptions.ArrayStrategy;
import boomerang.callgraph.BackwardsObservableICFG;
import boomerang.callgraph.ObservableDynamicICFG;
import boomerang.callgraph.ObservableICFG;
import boomerang.callgraph.ObservableStaticICFG;
import boomerang.controlflowgraph.DynamicCFG;
import boomerang.controlflowgraph.ObservableControlFlowGraph;
import boomerang.controlflowgraph.StaticCFG;
import boomerang.customize.BackwardEmptyCalleeFlow;
import boomerang.customize.EmptyCalleeFlow;
import boomerang.customize.ForwardEmptyCalleeFlow;
import boomerang.debugger.Debugger;
import boomerang.poi.AbstractPOI;
import boomerang.poi.CopyAccessPathChain;
import boomerang.poi.ExecuteImportFieldStmtPOI;
import boomerang.poi.PointOfIndirection;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.AllocVal;
import boomerang.scene.CallGraph;
import boomerang.scene.CallSiteStatement;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Field;
import boomerang.scene.Field.ArrayField;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Pair;
import boomerang.scene.ReturnSiteStatement;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver.ControlFlowLatticeElement;
import boomerang.solver.ForwardBoomerangSolver.KillElement;
import boomerang.solver.ForwardBoomerangSolver.UnknownElement;
import boomerang.solver.StatementBasedFieldTransitionListener;
import boomerang.solver.Strategies;
import boomerang.stats.IBoomerangStats;
import boomerang.util.DefaultValueMap;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sync.pds.solver.SyncPDSSolver.PDSSystem;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.NestedWeightedPAutomatons;
import wpds.impl.Rule;
import wpds.impl.SummaryNestedWeightedPAutomatons;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;

public abstract class WeightedBoomerang<W extends Weight> {
  protected ObservableICFG<Statement, Method> icfg;
  protected ObservableControlFlowGraph cfg;
  private static final Logger LOGGER = LoggerFactory.getLogger(WeightedBoomerang.class);
  private Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> genField =
      new HashMap<>();
  private long lastTick;
  private IBoomerangStats<W> stats;
  private Set<Method> visitedMethods = Sets.newHashSet();
  private Set<SolverCreationListener<W>> solverCreationListeners = Sets.newHashSet();
  private Multimap<SolverPair, ExecuteImportFieldStmtPOI<W>> poiListeners = HashMultimap.create();
  private Multimap<SolverPair, INode<Node<Statement, Val>>> activatedPoi = HashMultimap.create();
  private final DefaultValueMap<ForwardQuery, ForwardBoomerangSolver<W>> queryToSolvers =
      new DefaultValueMap<ForwardQuery, ForwardBoomerangSolver<W>>() {
        @Override
        protected ForwardBoomerangSolver<W> createItem(final ForwardQuery key) {
          final ForwardBoomerangSolver<W> solver;
          LOGGER.trace("Forward solving query: {}", key);
          forwardQueries++;
          solver = createForwardSolver(key);

          stats.registerSolver(key, solver);
          solver.getCallAutomaton().registerListener((t, w, aut) -> checkTimeout());
          solver.getFieldAutomaton().registerListener((t, w, aut) -> checkTimeout());
          onCreateSubSolver(key, solver);
          return solver;
        }
      };
  private int forwardQueries;
  private int backwardQueries;
  private final QueryGraph<W> queryGraph;
  private final DefaultValueMap<BackwardQuery, BackwardBoomerangSolver<W>> queryToBackwardSolvers =
      new DefaultValueMap<BackwardQuery, BackwardBoomerangSolver<W>>() {
        @Override
        protected BackwardBoomerangSolver<W> createItem(BackwardQuery key) {

          if (backwardSolverIns != null) {
            return backwardSolverIns;
          }
          BackwardBoomerangSolver<W> backwardSolver =
              new BackwardBoomerangSolver<W>(
                  bwicfg(),
                  cfg(),
                  genField,
                  key,
                  WeightedBoomerang.this.options,
                  createCallSummaries(null, backwardCallSummaries),
                  createFieldSummaries(null, backwardFieldSummaries),
                  WeightedBoomerang.this.dataFlowscope,
                  strategies,
                  null) {

                @Override
                protected Collection<? extends State> getEmptyCalleeFlow(
                    Method caller, Statement callSite, Val value, Statement returnSite) {
                  return backwardEmptyCalleeFlow.getEmptyCalleeFlow(
                      caller, callSite, value, returnSite);
                }

                @Override
                public WeightFunctions<Statement, Val, Field, W> getFieldWeights() {
                  return WeightedBoomerang.this.getBackwardFieldWeights();
                }

                @Override
                public WeightFunctions<Statement, Val, Statement, W> getCallWeights() {
                  return WeightedBoomerang.this.getBackwardCallWeights();
                }

                @Override
                protected boolean forceUnbalanced(INode<Val> node, Collection<INode<Val>> sources) {
                  return sources.contains(rootQuery) && callAutomaton.isUnbalancedState(node);
                }

                @Override
                protected boolean preventCallTransitionAdd(
                    Transition<Statement, INode<Val>> t, W weight) {
                  checkTimeout();
                  return super.preventCallTransitionAdd(t, weight);
                }

                @Override
                protected boolean preventFieldTransitionAdd(
                    Transition<Field, INode<Node<Statement, Val>>> t, W weight) {
                  checkTimeout();
                  return super.preventFieldTransitionAdd(t, weight);
                }
              };
          backwardSolver.registerListener(
              node -> {
                Optional<AllocVal> allocNode = isAllocationNode(node.stmt(), node.fact());
                if (allocNode.isPresent()
                    || node.stmt().isArrayLoad()
                    || node.stmt().isFieldLoad()) {
                  backwardSolver
                      .getFieldAutomaton()
                      .registerListener(new EmptyFieldListener(key, node));
                }
                addVisitedMethod(node.stmt().getMethod());

                if (options.handleMaps()) {
                  handleMapsBackward(node);
                }
              });
          backwardSolverIns = backwardSolver;
          return backwardSolver;
        }
      };

  private static final String MAP_PUT_SUB_SIGNATURE =
      "<java.util.Map: java.lang.Object put(java.lang.Object,java.lang.Object)>";
  private static final String MAP_GET_SUB_SIGNATURE =
      "<java.util.Map: java.lang.Object get(java.lang.Object)>";

  protected void handleMapsBackward(Node<Statement, Val> node) {
    if (node.stmt() instanceof ReturnSiteStatement) {
      ReturnSiteStatement rstmt = ((ReturnSiteStatement) node.stmt());
      Statement unwrap = rstmt.unwrap();
      if (unwrap.isAssign()
          && rstmt
              .getCallSiteStatement()
              .getInvokeExpr()
              .toString()
              .contains(MAP_GET_SUB_SIGNATURE)) {
        if (rstmt.getLeftOp().equals(node.fact())) {
          for (Statement s :
              rstmt.getMethod().getControlFlowGraph().getPredsOf(rstmt.getCallSiteStatement())) {

            BackwardQuery bwq =
                BackwardQuery.make(s, rstmt.getCallSiteStatement().getInvokeExpr().getArg(0));
            backwardSolve(bwq);
            for (ForwardQuery q : Lists.newArrayList(queryToSolvers.keySet())) {
              if (queryToSolvers.get(q).getReachedStates().contains(bwq.asNode())) {
                Val var = q.var();
                AllocVal v = (AllocVal) var;
                if (v.getAllocVal().isStringConstant()) {
                  String key = v.getAllocVal().getStringValue();
                  backwardSolverIns.propagate(
                      node,
                      new PushNode<>(
                          s,
                          rstmt.getCallSiteStatement().getInvokeExpr().getBase(),
                          Field.string(key),
                          PDSSystem.FIELDS));
                }
              }
            }
          }
        }
      }
      if (rstmt.getCallSiteStatement().getInvokeExpr().toString().contains(MAP_PUT_SUB_SIGNATURE)) {
        if (rstmt.getCallSiteStatement().getInvokeExpr().getBase().equals(node.fact())) {
          for (Statement s :
              rstmt.getMethod().getControlFlowGraph().getPredsOf(rstmt.getCallSiteStatement())) {
            BackwardQuery bwq =
                BackwardQuery.make(s, rstmt.getCallSiteStatement().getInvokeExpr().getArg(0));
            backwardSolve(bwq);
            for (ForwardQuery q : Lists.newArrayList(queryToSolvers.keySet())) {
              if (queryToSolvers.get(q).getReachedStates().contains(bwq.asNode())) {
                Val var = q.var();
                AllocVal v = (AllocVal) var;
                if (v.getAllocVal().isStringConstant()) {
                  String key = v.getAllocVal().getStringValue();
                  NodeWithLocation<Statement, Val, Field> succNode =
                      new NodeWithLocation<>(
                          s,
                          rstmt.getCallSiteStatement().getInvokeExpr().getArg(1),
                          Field.string(key));
                  backwardSolverIns.propagate(node, new PopNode<>(succNode, PDSSystem.FIELDS));
                }
              }
            }
          }
        }
      }
    }
  }

  protected void handleMapsForward(ForwardBoomerangSolver<W> solver, Node<Statement, Val> node) {
    if (node.stmt() instanceof CallSiteStatement) {
      CallSiteStatement rstmt = ((CallSiteStatement) node.stmt());
      Statement unwrap = rstmt.unwrap();
      if (unwrap.isAssign() && rstmt.getInvokeExpr().toString().contains(MAP_GET_SUB_SIGNATURE)) {
        if (rstmt.getInvokeExpr().getBase().equals(node.fact())) {
          BackwardQuery bwq = BackwardQuery.make(rstmt, rstmt.getInvokeExpr().getArg(0));
          backwardSolve(bwq);
          for (ForwardQuery q : Lists.newArrayList(queryToSolvers.keySet())) {
            if (queryToSolvers.get(q).getReachedStates().contains(bwq.asNode())) {
              Val var = q.var();
              AllocVal v = (AllocVal) var;
              if (v.getAllocVal().isStringConstant()) {
                String key = v.getAllocVal().getStringValue();
                NodeWithLocation<Statement, Val, Field> succNode =
                    new NodeWithLocation<>(
                        rstmt.getReturnSiteStatement(), unwrap.getLeftOp(), Field.string(key));
                solver.propagate(node, new PopNode<>(succNode, PDSSystem.FIELDS));
              }
            }
          }
        }
      }
      if (rstmt.getInvokeExpr().toString().contains(MAP_PUT_SUB_SIGNATURE)) {
        if (rstmt.getInvokeExpr().getArg(1).equals(node.fact())) {

          BackwardQuery bwq = BackwardQuery.make(rstmt, rstmt.getInvokeExpr().getArg(0));
          backwardSolve(bwq);
          for (ForwardQuery q : Lists.newArrayList(queryToSolvers.keySet())) {
            if (queryToSolvers.get(q).getReachedStates().contains(bwq.asNode())) {
              Val var = q.var();
              AllocVal v = (AllocVal) var;
              if (v.getAllocVal().isStringConstant()) {
                String key = v.getAllocVal().getStringValue();
                solver.propagate(
                    node,
                    new PushNode<>(
                        rstmt.getReturnSiteStatement(),
                        rstmt.getInvokeExpr().getBase(),
                        Field.string(key),
                        PDSSystem.FIELDS));
              }
            }
          }
        }
      }
    }
  }

  private BackwardBoomerangSolver<W> backwardSolverIns;
  private boolean solving;

  public void checkTimeout() {
    if (options.analysisTimeoutMS() > 0) {
      long elapsed = analysisWatch.elapsed(TimeUnit.MILLISECONDS);
      if (elapsed - lastTick > 15000) {
        LOGGER.debug(
            "Elapsed Time: {}/{}, Visited Methods {}",
            elapsed,
            options.analysisTimeoutMS(),
            visitedMethods.size());
        LOGGER.debug("Forward / Backward Queries: {}/{}", forwardQueries, backwardQueries);

        if (LOGGER.isDebugEnabled()) {
          printElapsedTimes();
          printRules();
          printStats();
        }
        lastTick = elapsed;
      }
      if (options.analysisTimeoutMS() < elapsed) {
        if (analysisWatch.isRunning()) analysisWatch.stop();
        throw new BoomerangTimeoutException(elapsed, stats);
      }
    }
  }

  private ObservableICFG<Statement, Method> bwicfg;
  private EmptyCalleeFlow forwardEmptyCalleeFlow = new ForwardEmptyCalleeFlow();
  private EmptyCalleeFlow backwardEmptyCalleeFlow = new BackwardEmptyCalleeFlow();

  private NestedWeightedPAutomatons<Statement, INode<Val>, W> backwardCallSummaries =
      new SummaryNestedWeightedPAutomatons<>();
  private NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> backwardFieldSummaries =
      new SummaryNestedWeightedPAutomatons<>();
  private NestedWeightedPAutomatons<Statement, INode<Val>, W> forwardCallSummaries =
      new SummaryNestedWeightedPAutomatons<>();
  private NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> forwardFieldSummaries =
      new SummaryNestedWeightedPAutomatons<>();
  private DefaultValueMap<FieldWritePOI, FieldWritePOI> fieldWrites =
      new DefaultValueMap<FieldWritePOI, FieldWritePOI>() {
        @Override
        protected FieldWritePOI createItem(FieldWritePOI key) {
          stats.registerFieldWritePOI(key);
          return key;
        }
      };
  protected final BoomerangOptions options;
  private Stopwatch analysisWatch = Stopwatch.createUnstarted();
  private final DataFlowScope dataFlowscope;
  private Strategies<W> strategies;
  private CallGraph callGraph;
  private INode<Val> rootQuery;

  public WeightedBoomerang(CallGraph cg, DataFlowScope scope, BoomerangOptions options) {
    this.options = options;
    this.options.checkValid();
    this.stats = options.statsFactory();
    this.dataFlowscope = scope;

    if (options.onTheFlyControlFlow()) {
      this.cfg = new DynamicCFG();
    } else {
      this.cfg = new StaticCFG();
    }

    if (options.onTheFlyCallGraph()) {
      icfg = new ObservableDynamicICFG(cfg, options.getResolutionStrategy().newInstance(this, cg));
    } else {
      icfg = new ObservableStaticICFG(cg);
    }
    this.callGraph = cg;
    this.strategies = new Strategies<>(options, this);
    this.queryGraph = new QueryGraph<>(this);
  }

  public WeightedBoomerang(CallGraph cg, DataFlowScope scope) {
    this(cg, scope, new DefaultBoomerangOptions());
  }

  protected void addVisitedMethod(Method method) {
    if (!dataFlowscope.isExcluded(method) && visitedMethods.add(method)) {
      LOGGER.trace("Reach Method: {}", method);
    }
  }

  protected Optional<AllocVal> isAllocationNode(Statement s, Val fact) {
    return options.getAllocationVal(s.getMethod(), s, fact, icfg());
  }

  protected ForwardBoomerangSolver<W> createForwardSolver(final ForwardQuery sourceQuery) {
    final ForwardBoomerangSolver<W> solver =
        new ForwardBoomerangSolver<W>(
            icfg(),
            cfg(),
            sourceQuery,
            genField,
            options,
            createCallSummaries(sourceQuery, forwardCallSummaries),
            createFieldSummaries(sourceQuery, forwardFieldSummaries),
            dataFlowscope,
            strategies,
            sourceQuery.getType()) {

          @Override
          protected Collection<? extends State> getEmptyCalleeFlow(
              Method caller, Statement callSite, Val value, Statement returnSite) {
            return forwardEmptyCalleeFlow.getEmptyCalleeFlow(caller, callSite, value, returnSite);
          }

          @Override
          public WeightFunctions<Statement, Val, Statement, W> getCallWeights() {
            return WeightedBoomerang.this.getForwardCallWeights(sourceQuery);
          }

          @Override
          public WeightFunctions<Statement, Val, Field, W> getFieldWeights() {
            return WeightedBoomerang.this.getForwardFieldWeights();
          }

          @Override
          public void addCallRule(Rule<Statement, INode<Val>, W> rule) {
            if (preventCallRuleAdd(sourceQuery, rule)) {
              return;
            }
            super.addCallRule(rule);
          }

          @Override
          protected boolean forceUnbalanced(INode<Val> node, Collection<INode<Val>> sources) {
            return queryGraph.isRoot(sourceQuery);
          }

          @Override
          protected void overwriteFieldAtStatement(
              Statement fieldWriteStatement,
              Transition<Field, INode<Node<Statement, Val>>> killedTransition) {
            BackwardQuery backwardQuery =
                BackwardQuery.make(fieldWriteStatement, fieldWriteStatement.getRightOp());
            CopyAccessPathChain<W> copyAccessPathChain =
                new CopyAccessPathChain<>(
                    queryToSolvers.get(sourceQuery),
                    queryToBackwardSolvers.getOrCreate(backwardQuery),
                    fieldWriteStatement,
                    killedTransition);
            copyAccessPathChain.exec();
            queryGraph.addEdge(sourceQuery, killedTransition.getStart().fact(), backwardQuery);
          }

          @Override
          protected void propagateUnbalancedToCallSite(
              CallSiteStatement callSite, Transition<Statement, INode<Val>> transInCallee) {
            if (options.onTheFlyControlFlow()
                && visited.add(new Pair<>(callSite, callSite.getReturnSiteStatement()))) {
              if (visited.add(
                  new Pair<>(
                      callSite.getReturnSiteStatement(), callSite.getReturnSiteStatement()))) {
                worklist.add(callSite.getReturnSiteStatement());
              }
            }
            super.propagateUnbalancedToCallSite(callSite, transInCallee);
          }

          @Override
          public Collection<? extends State> computeCallFlow(
              Method caller,
              CallSiteStatement callSite,
              ReturnSiteStatement returnSiteStatement,
              InvokeExpr invokeExpr,
              Node<Statement, Val> currNode,
              Method callee,
              Statement calleeSp) {
            // TODO Auto-generated method stub
            Collection<? extends State> out =
                super.computeCallFlow(
                    caller, callSite, returnSiteStatement, invokeExpr, currNode, callee, calleeSp);
            if (options.onTheFlyControlFlow() && !out.isEmpty()) {
              if (visited.add(new Pair<>(calleeSp, calleeSp))) {
                worklist.add(calleeSp);
              }
            }
            return out;
          }

          @Override
          protected boolean preventCallTransitionAdd(
              Transition<Statement, INode<Val>> t, W weight) {
            checkTimeout();
            return super.preventCallTransitionAdd(t, weight);
          }

          @Override
          protected boolean preventFieldTransitionAdd(
              Transition<Field, INode<Node<Statement, Val>>> t, W weight) {
            checkTimeout();
            return super.preventFieldTransitionAdd(t, weight);
          }
        };

    solver.registerListener(
        node -> {
          if (node.stmt().isFieldStore()) {
            forwardHandleFieldWrite(node, createFieldStore(node.stmt()), sourceQuery);
          } else if (options.getArrayStrategy() != ArrayStrategy.DISABLED
              && node.stmt().isArrayStore()) {
            forwardHandleFieldWrite(node, createArrayFieldStore(node.stmt()), sourceQuery);
          }

          addVisitedMethod(node.stmt().getMethod());

          if (options.handleMaps()) {
            handleMapsForward(solver, node);
          }
        });

    return solver;
  }

  private NestedWeightedPAutomatons<Statement, INode<Val>, W> createCallSummaries(
      final ForwardQuery sourceQuery,
      final NestedWeightedPAutomatons<Statement, INode<Val>, W> summaries) {
    return new NestedWeightedPAutomatons<Statement, INode<Val>, W>() {

      @Override
      public void putSummaryAutomaton(
          INode<Val> target, WeightedPAutomaton<Statement, INode<Val>, W> aut) {
        summaries.putSummaryAutomaton(target, aut);
      }

      @Override
      public WeightedPAutomaton<Statement, INode<Val>, W> getSummaryAutomaton(INode<Val> target) {
        if (sourceQuery.var() instanceof AllocVal) {
          AllocVal allocVal = (AllocVal) sourceQuery.var();
          Val f;
          if (target.fact().isUnbalanced()) {
            f = target.fact().asUnbalanced(null);
          } else {
            f = target.fact();
          }
          if (f.equals(allocVal.getDelegate())) {
            return queryToSolvers.getOrCreate(sourceQuery).getCallAutomaton();
          }
        }
        return summaries.getSummaryAutomaton(target);
      }
    };
  }

  private NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> createFieldSummaries(
      final ForwardQuery sourceQuery,
      final NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> summaries) {
    return new NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W>() {

      @Override
      public void putSummaryAutomaton(
          INode<Node<Statement, Val>> target,
          WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
        summaries.putSummaryAutomaton(target, aut);
      }

      @Override
      public WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> getSummaryAutomaton(
          INode<Node<Statement, Val>> target) {
        if (target.fact().equals(sourceQuery.asNode())) {
          return queryToSolvers.getOrCreate(sourceQuery).getFieldAutomaton();
        }
        return summaries.getSummaryAutomaton(target);
      }
    };
  }

  public boolean preventCallRuleAdd(ForwardQuery sourceQuery, Rule<Statement, INode<Val>, W> rule) {
    return false;
  }

  protected FieldWritePOI createArrayFieldStore(Statement s) {
    Pair<Val, Integer> base = s.getArrayBase();
    return fieldWrites.getOrCreate(
        new FieldWritePOI(s, base.getX(), Field.array(base.getY()), s.getRightOp()));
  }

  protected FieldWritePOI createFieldStore(Statement s) {
    Val base = s.getFieldStore().getX();
    Field field = s.getFieldStore().getY();
    Val stored = s.getRightOp();
    return fieldWrites.getOrCreate(new FieldWritePOI(s, base, field, stored));
  }

  protected void forwardHandleFieldWrite(
      final Node<Statement, Val> node,
      final FieldWritePOI fieldWritePoi,
      final ForwardQuery sourceQuery) {
    BackwardQuery backwardQuery = BackwardQuery.make(node.stmt(), fieldWritePoi.getBaseVar());
    if (node.fact().equals(fieldWritePoi.getStoredVar())) {
      backwardSolve(backwardQuery);
      queryGraph.addEdge(sourceQuery, node, backwardQuery);
      queryToSolvers
          .get(sourceQuery)
          .registerStatementFieldTransitionListener(
              new ForwardHandleFieldWrite(sourceQuery, fieldWritePoi, node.stmt()));
    }
    if (node.fact().equals(fieldWritePoi.getBaseVar())) {
      queryToSolvers
          .getOrCreate(sourceQuery)
          .getFieldAutomaton()
          .registerListener(
              new TriggerBaseAllocationAtFieldWrite(
                  new SingleNode<>(node), fieldWritePoi, sourceQuery));
    }
  }

  public void unregisterAllListeners() {
    for (AbstractBoomerangSolver<W> solver : queryToSolvers.values()) {
      solver.unregisterAllListeners();
    }
    for (AbstractBoomerangSolver<W> solver : queryToBackwardSolvers.values()) {
      solver.unregisterAllListeners();
    }
    this.cfg.unregisterAllListeners();
    this.queryGraph.unregisterAllListeners();
    this.poiListeners.clear();
    this.worklist.clear();
    this.activatedPoi.clear();
    this.fieldWrites.clear();
  }

  public DefaultValueMap<BackwardQuery, BackwardBoomerangSolver<W>> getBackwardSolvers() {
    return queryToBackwardSolvers;
  }

  private final class EmptyFieldListener
      extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

    private BackwardQuery key;
    private Node<Statement, Val> node;

    public EmptyFieldListener(BackwardQuery key, Node<Statement, Val> node) {
      super(new SingleNode<>(node));
      this.key = key;
      this.node = node;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + getEnclosingInstance().hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      EmptyFieldListener other = (EmptyFieldListener) obj;
      if (!getEnclosingInstance().equals(other.getEnclosingInstance())) return false;
      return true;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
      if (!t.getLabel().equals(Field.empty()) && !(t.getLabel() instanceof ArrayField)) return;
      Optional<AllocVal> allocNode = isAllocationNode(node.stmt(), node.fact());
      if (allocNode.isPresent()) {
        AllocVal val = allocNode.get();
        ForwardQuery forwardQuery;
        if (t.getLabel() instanceof ArrayField) {
          WeightedBoomerang.this
              .backwardSolverIns
              .getFieldAutomaton()
              .registerListener(
                  new ArrayAllocationListener(
                      ((ArrayField) t.getLabel()).getIndex(), t.getTarget(), val, key, node));
        } else {
          forwardQuery = new ForwardQuery(node.stmt(), val);
          forwardSolve(forwardQuery);
          queryGraph.addEdge(key, node, forwardQuery);
        }
      }
    }

    @Override
    public void onInTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {}

    private WeightedBoomerang getEnclosingInstance() {
      return WeightedBoomerang.this;
    }
  }

  private final class ArrayAllocationListener
      extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

    private final int arrayAccessIndex;
    private AllocVal val;
    private BackwardQuery key;
    private Node<Statement, Val> node;

    public ArrayAllocationListener(
        int arrayAccessIndex,
        INode<Node<Statement, Val>> target,
        AllocVal val,
        BackwardQuery key,
        Node<Statement, Val> node) {
      super(target);
      this.arrayAccessIndex = arrayAccessIndex;
      this.val = val;
      this.key = key;
      this.node = node;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
      if (t.getLabel().equals(Field.empty())) {
        ForwardQueryArray forwardQuery = new ForwardQueryArray(node.stmt(), val, arrayAccessIndex);
        forwardSolve(forwardQuery);
        queryGraph.addEdge(key, node, forwardQuery);
      }
      if (t.getLabel() instanceof ArrayField) {
        ForwardQueryMultiDimensionalArray forwardQuery =
            new ForwardQueryMultiDimensionalArray(
                node.stmt(), val, arrayAccessIndex, ((ArrayField) t.getLabel()).getIndex());
        forwardSolve(forwardQuery);
        queryGraph.addEdge(key, node, forwardQuery);
      }
    }

    @Override
    public void onInTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {}

    private WeightedBoomerang getEnclosingInstance() {
      return WeightedBoomerang.this;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + getEnclosingInstance().hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      ArrayAllocationListener other = (ArrayAllocationListener) obj;
      if (!getEnclosingInstance().equals(other.getEnclosingInstance())) return false;
      return true;
    }
  }

  private final class ForwardHandleFieldWrite extends StatementBasedFieldTransitionListener<W> {
    private final Query sourceQuery;
    private final AbstractPOI<Statement, Val, Field> fieldWritePoi;
    private final Statement stmt;

    private ForwardHandleFieldWrite(
        Query sourceQuery, AbstractPOI<Statement, Val, Field> fieldWritePoi, Statement statement) {
      super(statement);
      this.sourceQuery = sourceQuery;
      this.fieldWritePoi = fieldWritePoi;
      this.stmt = statement;
    }

    @Override
    public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
      if (t.getStart() instanceof GeneratedState) return;
      if (t.getStart().fact().stmt().equals(stmt)) {
        fieldWritePoi.addFlowAllocation(sourceQuery);
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((sourceQuery == null) ? 0 : sourceQuery.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ForwardHandleFieldWrite other = (ForwardHandleFieldWrite) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (sourceQuery == null) {
        if (other.sourceQuery != null) return false;
      } else if (!sourceQuery.equals(other.sourceQuery)) return false;
      return true;
    }

    private WeightedBoomerang getOuterType() {
      return WeightedBoomerang.this;
    }
  }

  private class TriggerBaseAllocationAtFieldWrite
      extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

    private final PointOfIndirection<Statement, Val, Field> fieldWritePoi;
    private final ForwardQuery sourceQuery;

    public TriggerBaseAllocationAtFieldWrite(
        INode<Node<Statement, Val>> state,
        PointOfIndirection<Statement, Val, Field> fieldWritePoi,
        ForwardQuery sourceQuery) {
      super(state);
      this.fieldWritePoi = fieldWritePoi;
      this.sourceQuery = sourceQuery;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
      if (isAllocationNode(t.getTarget().fact().fact(), sourceQuery)) {
        fieldWritePoi.addBaseAllocation(sourceQuery);
      }
    }

    @Override
    public void onInTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {}

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((fieldWritePoi == null) ? 0 : fieldWritePoi.hashCode());
      result = prime * result + ((sourceQuery == null) ? 0 : sourceQuery.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      TriggerBaseAllocationAtFieldWrite other = (TriggerBaseAllocationAtFieldWrite) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (fieldWritePoi == null) {
        if (other.fieldWritePoi != null) return false;
      } else if (!fieldWritePoi.equals(other.fieldWritePoi)) return false;
      if (sourceQuery == null) {
        if (other.sourceQuery != null) return false;
      } else if (!sourceQuery.equals(other.sourceQuery)) return false;
      return true;
    }

    private WeightedBoomerang getOuterType() {
      return WeightedBoomerang.this;
    }
  }

  private boolean isAllocationNode(Val fact, ForwardQuery sourceQuery) {
    /*TODO Replace by new designated type: see AbstractBoomerangSolver*/
    return fact.equals(sourceQuery.var().asUnbalanced(sourceQuery.stmt()));
  }

  private ObservableICFG<Statement, Method> bwicfg() {
    if (bwicfg == null) bwicfg = new BackwardsObservableICFG(icfg());
    return bwicfg;
  }

  public ForwardBoomerangResults<W> solve(ForwardQuery query) {
    if (!options.allowMultipleQueries() && solving) {
      throw new RuntimeException(
          "One cannot re-use the same Boomerang solver for more than one query, unless option allowMultipleQueries is enabled. If allowMultipleQueries is enabled, ensure to call unregisterAllListeners() on this instance upon termination of all queries.");
    }
    solving = true;
    if (!analysisWatch.isRunning()) {
      analysisWatch.start();
    }
    boolean timedout = false;
    try {
      queryGraph.addRoot(query);
      LOGGER.trace("Starting forward analysis of: {}", query);
      forwardSolve(query);
      LOGGER.trace(
          "Query terminated in {} ({}), visited methods {}",
          analysisWatch,
          query,
          visitedMethods.size());
      LOGGER.trace("Query Graph \n{}", queryGraph.toDotString());
      icfg.computeFallback();
    } catch (BoomerangTimeoutException e) {
      timedout = true;
      LOGGER.trace(
          "Timeout ({}) of query: {}, visited methods {}",
          analysisWatch,
          query,
          visitedMethods.size());
    } catch (Throwable e) {
      LOGGER.error("Solving query crashed in {}", e);
    }
    if (!options.allowMultipleQueries()) {
      unregisterAllListeners();
    }

    if (analysisWatch.isRunning()) {
      analysisWatch.stop();
    }
    return new ForwardBoomerangResults<W>(
        query,
        icfg(),
        cfg(),
        timedout,
        this.queryToSolvers,
        getStats(),
        analysisWatch,
        visitedMethods,
        options.trackDataFlowPath(),
        options.prunePathConditions(),
        options.trackImplicitFlows());
  }

  public BackwardBoomerangResults<W> solve(BackwardQuery query) {
    return solve(query, true);
  }

  public BackwardBoomerangResults<W> solve(BackwardQuery query, boolean timing) {
    if (!options.allowMultipleQueries() && solving) {
      throw new RuntimeException(
          "One cannot re-use the same Boomerang solver for more than one query, unless option allowMultipleQueries is enabled. If allowMultipleQueries is enabled, ensure to call unregisterAllListeners() on this instance upon termination of all queries.");
    }
    solving = true;
    if (timing && !analysisWatch.isRunning()) {
      analysisWatch.start();
    }
    boolean timedout = false;
    try {
      queryGraph.addRoot(query);
      LOGGER.trace("Starting backward analysis of: {}", query);
      backwardSolve(query);
    } catch (BoomerangTimeoutException e) {
      timedout = true;
      LOGGER.info("Timeout ({}) of query: {} ", analysisWatch, query);
    } catch (Throwable e) {
      System.err.println("Boomerang CRASHED" + e);
      LOGGER.error("Boomerang crashed in {} ", e);
    }
    if (!options.allowMultipleQueries()) {
      unregisterAllListeners();
    }
    if (timing && analysisWatch.isRunning()) {
      analysisWatch.stop();
    }
    return new BackwardBoomerangResults<W>(
        query, timedout, this.queryToSolvers, backwardSolverIns, getStats(), analysisWatch);
  }

  public void debugOutput() {
    LOGGER.trace("Query Graph \n{}", queryGraph.toDotString());
    // LOGGER.trace("Terminated backward analysis of: {}", query);
    LOGGER.trace("#ForwardSolvers: {}", queryToSolvers.size());
    printAllAutomata();
    printAllForwardCallAutomatonFlow();
    printAllBackwardCallAutomatonFlow();
  }

  protected void backwardSolve(BackwardQuery query) {
    if (!options.aliasing()) return;
    AbstractBoomerangSolver<W> solver = queryToBackwardSolvers.getOrCreate(query);
    INode<Node<Statement, Val>> fieldTarget = solver.createQueryNodeField(query);
    INode<Val> callTarget = solver.generateCallState(new SingleNode<>(query.var()), query.stmt());
    if (rootQuery == null) {
      rootQuery = callTarget;
    }
    solver.solve(query.asNode(), Field.empty(), fieldTarget, query.stmt(), callTarget);
  }

  private AbstractBoomerangSolver<W> forwardSolve(ForwardQuery query) {
    Statement stmt = query.asNode().stmt();
    AbstractBoomerangSolver<W> solver = queryToSolvers.getOrCreate(query);
    INode<Node<Statement, Val>> fieldTarget = solver.createQueryNodeField(query);
    INode<Val> callTarget = solver.generateCallState(new SingleNode<>(query.var()), query.stmt());
    if (!(stmt.isFieldStore())
        && query instanceof ForwardQueryArray
        && options.getArrayStrategy() != ArrayStrategy.DISABLED) {
      if (query instanceof ForwardQueryMultiDimensionalArray) {
        ForwardQueryMultiDimensionalArray arrayQuery = ((ForwardQueryMultiDimensionalArray) query);
        Node<Statement, Val> node =
            new Node<>(query.stmt(), ((AllocVal) query.var()).getDelegate());
        SingleNode<Node<Statement, Val>> sourveVal = new SingleNode<>(node);
        INode<Node<Statement, Val>> genState1 =
            solver.generateFieldState(sourveVal, Field.array(arrayQuery.getIndex1()));
        insertTransition(
            solver.getFieldAutomaton(),
            new Transition<>(sourveVal, Field.array(arrayQuery.getIndex1()), genState1));
        INode<Node<Statement, Val>> genState2 =
            solver.generateFieldState(sourveVal, Field.array(arrayQuery.getIndex2()));
        insertTransition(
            solver.getFieldAutomaton(),
            new Transition<>(genState1, Field.array(arrayQuery.getIndex2()), genState2));
        insertTransition(
            solver.getFieldAutomaton(), new Transition<>(genState2, Field.empty(), fieldTarget));
      } else {
        ForwardQueryArray arrayQuery = ((ForwardQueryArray) query);
        Node<Statement, Val> node =
            new Node<>(query.stmt(), ((AllocVal) query.var()).getDelegate());
        SingleNode<Node<Statement, Val>> sourceVal = new SingleNode<>(node);
        INode<Node<Statement, Val>> genState =
            solver.generateFieldState(sourceVal, Field.array(arrayQuery.getIndex()));
        insertTransition(
            solver.getFieldAutomaton(),
            new Transition<>(sourceVal, Field.array(arrayQuery.getIndex()), genState));
        insertTransition(
            solver.getFieldAutomaton(), new Transition<>(genState, Field.empty(), fieldTarget));
      }
    }
    if (stmt.isStringAllocation()) {
      // Scene.v().forceResolve("java.lang.String",
      // SootClass.BODIES);
      //        	throw new RuntimeException("Not properly implemented String allocation site");
      //            SootClass stringClass = Scene.v().getSootClass("java.lang.String");
      //            if (stringClass.declaresField("char[] value")) {
      //                SootField valueField = stringClass.getField("char[] value");
      //                SingleNode<Node<Statement, Val>> s = new SingleNode<Node<Statement,
      // Val>>(query.asNode());
      //                INode<Node<Statement, Val>> irState =
      // solver.getFieldAutomaton().createState(s,
      //                        new Field(valueField));
      //                insertTransition(solver.getFieldAutomaton(), new Transition<Field,
      // INode<Node<Statement, Val>>>(
      //                        new SingleNode<Node<Statement, Val>>(query.asNode()), new
      // Field(valueField), irState));
      //                insertTransition(solver.getFieldAutomaton(), new Transition<Field,
      // INode<Node<Statement, Val>>>(
      //                        irState, Field.empty(),
      // solver.getFieldAutomaton().getInitialState()));
      //            }
    }
    Val var;
    Field field;
    if (stmt.isFieldStore()) {
      field = stmt.getFieldStore().getY();
      var = stmt.getFieldStore().getX();
      forwardHandleFieldWrite(
          new Node<>(stmt, stmt.getRightOp()),
          new FieldWritePOI(stmt, var, field, stmt.getRightOp()),
          query);
    } else {
      var = ((AllocVal) query.var()).getDelegate();
      field = Field.empty();
    }
    if (query instanceof WeightedForwardQuery) {
      WeightedForwardQuery<W> q = (WeightedForwardQuery<W>) query;
      // Convert AllocVal -> Val
      solver.solve(new Node<>(stmt, var), field, fieldTarget, stmt, callTarget, q.weight());
    } else {
      // Convert AllocVal -> Val
      solver.solve(new Node<>(stmt, var), field, fieldTarget, stmt, callTarget);
    }

    if (options.onTheFlyControlFlow()) {
      worklist.add(stmt);
      controlFlowAnalysis();
    }
    return solver;
  }

  Set<Pair<Statement, Statement>> visited = Sets.newHashSet();
  LinkedList<Statement> worklist = Lists.newLinkedList();

  private void controlFlowAnalysis() {
    if (!options.onTheFlyControlFlow()) {
      return;
    }
    while (!worklist.isEmpty()) {
      Statement first = worklist.pop();
      Collection<Statement> succs = first.getMethod().getControlFlowGraph().getSuccsOf(first);
      for (Statement succ : succs) {
        ControlFlowLatticeElement latticeEl = UnknownElement.create();
        for (ForwardQuery q : Lists.newArrayList(queryToSolvers.keySet())) {
          ForwardBoomerangSolver<W> solver = queryToSolvers.get(q);
          latticeEl = latticeEl.merge(solver.controlFlowStep(first, succ, succs));
        }
        if (visited.add(new Pair<>(first, succ))) {
          if (!(latticeEl instanceof KillElement)) {
            cfg.step(first, succ);
            worklist.add(succ);
          } else {
            LOGGER.trace("Killing control flow from {} to {}", first, succ);
          }
        }
      }
    }
  }

  private boolean insertTransition(
      final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut,
      Transition<Field, INode<Node<Statement, Val>>> transition) {
    if (!aut.nested()) {
      return aut.addTransition(transition);
    }
    INode<Node<Statement, Val>> target = transition.getTarget();
    if (!(target instanceof GeneratedState)) {
      forwardFieldSummaries.putSummaryAutomaton(target, aut);

      aut.registerListener(
          (t, w, aut12) -> {
            if (t.getStart() instanceof GeneratedState) {
              WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> n =
                  forwardFieldSummaries.getSummaryAutomaton(t.getStart());
              aut12.addNestedAutomaton(n);
            }
          });
      return aut.addTransition(transition);
    }
    final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> nested =
        forwardFieldSummaries.getSummaryAutomaton(target);
    nested.registerListener(
        (t, w, aut1) -> {
          if (t.getStart() instanceof GeneratedState) {
            WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> n =
                forwardFieldSummaries.getSummaryAutomaton(t.getStart());
            aut1.addNestedAutomaton(n);
          }
        });
    return nested.addTransition(transition);
  }

  public class FieldWritePOI extends AbstractPOI<Statement, Val, Field> {

    public FieldWritePOI(Statement statement, Val base, Field field, Val stored) {
      super(statement, base, field, stored);
    }

    @Override
    public void execute(final ForwardQuery baseAllocation, final Query flowAllocation) {
      if (flowAllocation instanceof BackwardQuery) {
      } else if (flowAllocation instanceof ForwardQuery) {
        ForwardBoomerangSolver<W> baseSolver = queryToSolvers.get(baseAllocation);
        ForwardBoomerangSolver<W> flowSolver = queryToSolvers.get(flowAllocation);
        ExecuteImportFieldStmtPOI<W> exec =
            new ExecuteImportFieldStmtPOI<W>(baseSolver, flowSolver, FieldWritePOI.this) {
              public void activate(INode<Node<Statement, Val>> start) {
                activateAllPois(new SolverPair(flowSolver, baseSolver), start);
              };
            };
        registerActivationListener(new SolverPair(flowSolver, baseSolver), exec);
        exec.solve();
      }
    }
  }

  protected void activateAllPois(SolverPair pair, INode<Node<Statement, Val>> start) {
    if (activatedPoi.put(pair, start)) {
      Collection<ExecuteImportFieldStmtPOI<W>> listeners = poiListeners.get(pair);
      for (ExecuteImportFieldStmtPOI<W> l : Lists.newArrayList(listeners)) {
        l.trigger(start);
      }
    }
  }

  public void registerActivationListener(SolverPair solverPair, ExecuteImportFieldStmtPOI<W> exec) {
    Collection<INode<Node<Statement, Val>>> listeners = activatedPoi.get(solverPair);
    for (INode<Node<Statement, Val>> node : Lists.newArrayList(listeners)) {
      exec.trigger(node);
    }
    poiListeners.put(solverPair, exec);
  }

  private class SolverPair {

    private AbstractBoomerangSolver<W> flowSolver;
    private AbstractBoomerangSolver<W> baseSolver;

    public SolverPair(
        AbstractBoomerangSolver<W> flowSolver, AbstractBoomerangSolver<W> baseSolver) {
      this.flowSolver = flowSolver;
      this.baseSolver = baseSolver;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((baseSolver == null) ? 0 : baseSolver.hashCode());
      result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      SolverPair other = (SolverPair) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (baseSolver == null) {
        if (other.baseSolver != null) return false;
      } else if (!baseSolver.equals(other.baseSolver)) return false;
      if (flowSolver == null) {
        if (other.flowSolver != null) return false;
      } else if (!flowSolver.equals(other.flowSolver)) return false;
      return true;
    }

    private WeightedBoomerang getOuterType() {
      return WeightedBoomerang.this;
    }
  }

  public ObservableICFG<Statement, Method> icfg() {
    return icfg;
  }

  public ObservableControlFlowGraph cfg() {
    return cfg;
  }

  protected abstract WeightFunctions<Statement, Val, Field, W> getForwardFieldWeights();

  protected abstract WeightFunctions<Statement, Val, Field, W> getBackwardFieldWeights();

  protected abstract WeightFunctions<Statement, Val, Statement, W> getBackwardCallWeights();

  protected abstract WeightFunctions<Statement, Val, Statement, W> getForwardCallWeights(
      ForwardQuery sourceQuery);

  public DefaultValueMap<ForwardQuery, ForwardBoomerangSolver<W>> getSolvers() {
    return queryToSolvers;
  }

  public void debugOutput(Debugger<W> debugger) {
    debugger.done(icfg, cfg, visitedMethods, queryToSolvers);
  }

  public IBoomerangStats<W> getStats() {
    return stats;
  }

  public void onCreateSubSolver(Query key, AbstractBoomerangSolver<W> solver) {
    for (SolverCreationListener<W> l : solverCreationListeners) {
      l.onCreatedSolver(key, solver);
    }
  }

  public void registerSolverCreationListener(SolverCreationListener<W> l) {
    if (solverCreationListeners.add(l)) {
      for (Entry<ForwardQuery, ForwardBoomerangSolver<W>> e :
          Lists.newArrayList(queryToSolvers.entrySet())) {
        l.onCreatedSolver(e.getKey(), e.getValue());
      }
    }
  }

  public Table<Statement, Val, W> getResults(ForwardQuery seed) {
    final Table<Statement, Val, W> results = HashBasedTable.create();
    WeightedPAutomaton<Statement, INode<Val>, W> fieldAut =
        queryToSolvers.getOrCreate(seed).getCallAutomaton();
    for (Entry<Transition<Statement, INode<Val>>, W> e :
        fieldAut.getTransitionsToFinalWeights().entrySet()) {
      Transition<Statement, INode<Val>> t = e.getKey();
      W w = e.getValue();
      if (t.getLabel().equals(Statement.epsilon())) continue;
      if (t.getStart().fact().isLocal()
          && !t.getLabel().getMethod().equals(t.getStart().fact().m())) continue;
      results.put(t.getLabel(), t.getStart().fact(), w);
    }
    return results;
  }

  public BoomerangOptions getOptions() {
    return this.options;
  }

  public CallGraph getCallGraph() {
    return this.callGraph;
  }

  public Set<Method> getVisitedMethods() {
    return visitedMethods;
  }

  // For debugging purpose

  public void printCallAutomatonFlow(AbstractBoomerangSolver<W> solver) {
    final Table<Statement, Val, W> results = HashBasedTable.create();
    WeightedPAutomaton<Statement, INode<Val>, W> callAut = solver.getCallAutomaton();
    for (Entry<Transition<Statement, INode<Val>>, W> e :
        callAut.getTransitionsToFinalWeights().entrySet()) {
      Transition<Statement, INode<Val>> t = e.getKey();
      W w = e.getValue();
      if (t.getLabel().equals(Statement.epsilon())) continue;
      if (t.getStart().fact().isLocal()
          && !t.getLabel().getMethod().equals(t.getStart().fact().m())) continue;
      results.put(t.getLabel(), t.getStart().fact(), w);
    }
    LOGGER.trace("Call Automaton flow for {}", solver);
    printResultsPerMethod(results);
  }

  private void printResultsPerMethod(Table<Statement, Val, W> results) {
    Multimap<Method, Cell<Statement, Val, W>> methodToRes = HashMultimap.create();
    for (Cell<Statement, Val, W> c : results.cellSet()) {
      methodToRes.put(c.getRowKey().getMethod(), c);
    }

    for (Method m : methodToRes.keySet()) {
      LOGGER.trace("Results in Method {}: ", m);
      for (Statement s : m.getStatements()) {
        LOGGER.trace("\tStatement {}: ", s);
        for (Cell<Statement, Val, W> c : methodToRes.get(m)) {
          if (c.getRowKey().equals(s)) {
            LOGGER.trace("\t\tVal: {}, W: {}", c.getColumnKey(), c.getValue());
          }
        }
      }
    }
  }

  public void printAllForwardCallAutomatonFlow() {
    for (Entry<ForwardQuery, ForwardBoomerangSolver<W>> e : queryToSolvers.entrySet()) {
      printCallAutomatonFlow(e.getValue());
    }
  }

  public void printAllBackwardCallAutomatonFlow() {
    for (Entry<BackwardQuery, BackwardBoomerangSolver<W>> e : queryToBackwardSolvers.entrySet()) {
      printCallAutomatonFlow(e.getValue());
    }
  }

  public void printAllAutomata() {
    for (Entry<ForwardQuery, ForwardBoomerangSolver<W>> e : queryToSolvers.entrySet()) {
      printAutomata(e.getKey());
    }

    LOGGER.trace("Backward Solver");

    for (Entry<BackwardQuery, BackwardBoomerangSolver<W>> e : queryToBackwardSolvers.entrySet()) {
      printAutomata(e.getKey());
    }
  }

  public void printAutomata(Query q) {
    if (LOGGER.isTraceEnabled() && q instanceof ForwardQuery) {
      LOGGER.trace("Solver {}", queryToSolvers.get(q).getQuery());
      LOGGER.trace("Field Automaton\n {}", queryToSolvers.get(q).getFieldAutomaton().toDotString());
      LOGGER.trace("Call Automaton\n {}", queryToSolvers.get(q).getCallAutomaton().toDotString());
    }
    if (LOGGER.isTraceEnabled() && q instanceof BackwardQuery) {
      LOGGER.trace("Solver {}", queryToBackwardSolvers.get(q));
      LOGGER.trace(
          "Field Automaton\n {}", queryToBackwardSolvers.get(q).getFieldAutomaton().toDotString());
      LOGGER.trace(
          "Call Automaton\n {}", queryToBackwardSolvers.get(q).getCallAutomaton().toDotString());
    }
  }

  private void printStats() {
    int forwardCallTransitions = 0;
    int backwardCallTransitions = 0;
    int forwardFieldTransitions = 0;
    int backwardFieldTransitions = 0;
    for (Entry<ForwardQuery, ForwardBoomerangSolver<W>> e : queryToSolvers.entrySet()) {
      if (e.getKey() instanceof ForwardQuery) {
        forwardCallTransitions += e.getValue().getCallAutomaton().getTransitions().size();
        forwardFieldTransitions += e.getValue().getFieldAutomaton().getTransitions().size();
      } else {
        backwardCallTransitions += e.getValue().getCallAutomaton().getTransitions().size();
        backwardFieldTransitions += e.getValue().getFieldAutomaton().getTransitions().size();
      }
    }
    LOGGER.trace("Forward Call Transitions: {}", forwardCallTransitions);
    LOGGER.trace("Forward Field Transitions: {}", forwardFieldTransitions);

    LOGGER.trace("Backward Call Transitions: {}", backwardCallTransitions);
    LOGGER.trace("Backward Field Transitions: {}", backwardFieldTransitions);
  }

  private void printQueryTimes() {
    for (Entry<ForwardQuery, ForwardBoomerangSolver<W>> entry : queryToSolvers.entrySet()) {
      LOGGER.trace("{}", entry.getKey());
      LOGGER.trace("Call Automaton Time: {}", entry.getValue().getCallAutomaton().getWatch());
      LOGGER.trace("Field Automaton Time: {}", entry.getValue().getFieldAutomaton().getWatch());
    }
  }

  private void printElapsedTimes() {
    // LOGGER.debug("BackwardCallWatch " + backwardSolver.getCallAutomaton().getWatch());
    // LOGGER.debug("BackwardFieldWatch " + backwardSolver.getFieldAutomaton().getWatch());
    long forwardCallElaps = 0;
    long forwardFieldElaps = 0;
    for (ForwardBoomerangSolver<W> v : queryToSolvers.values()) {
      forwardCallElaps += v.getCallAutomaton().getWatch().elapsed(TimeUnit.SECONDS);
      forwardFieldElaps += v.getFieldAutomaton().getWatch().elapsed(TimeUnit.SECONDS);
    }

    LOGGER.trace("ForwardCallWatch " + forwardCallElaps + " s");
    LOGGER.trace("ForwardFieldWatch " + forwardFieldElaps + " s");
  }

  private void printRules() {
    // LOGGER.debug("BackwardCallRules " + backwardSolver.getCallPDS().getAllRules().size());
    // LOGGER.debug("BackwardFieldRules " + backwardSolver.getFieldPDS().getAllRules().size());
    long forwardCallElaps = 0;
    long forwardFieldElaps = 0;
    Set<Rule> allCallRules = Sets.newHashSet();
    Set<Rule> allFieldRules = Sets.newHashSet();
    for (ForwardBoomerangSolver<W> v : queryToSolvers.values()) {
      allCallRules.addAll(v.getCallPDS().getAllRules());
      allFieldRules.addAll(v.getFieldPDS().getAllRules());
      forwardCallElaps += v.getCallPDS().getAllRules().size();
      forwardFieldElaps += v.getFieldPDS().getAllRules().size();
    }

    LOGGER.trace("ForwardCallRules (total)" + forwardCallElaps);
    LOGGER.trace("ForwardFieldRules (total)" + forwardFieldElaps);

    LOGGER.trace("ForwardCallRules (deduplicated)" + allCallRules.size());
    LOGGER.trace("ForwardFieldRules (deduplicated)" + allFieldRules.size());
  }
}
