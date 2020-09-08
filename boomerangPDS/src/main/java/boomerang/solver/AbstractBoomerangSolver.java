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
package boomerang.solver;

import boomerang.BoomerangOptions;
import boomerang.Query;
import boomerang.callgraph.BackwardsObservableICFG;
import boomerang.callgraph.CallerListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.controlflowgraph.ObservableControlFlowGraph;
import boomerang.controlflowgraph.PredecessorListener;
import boomerang.controlflowgraph.SuccessorListener;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Field;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Type;
import boomerang.scene.Val;
import boomerang.util.RegExAccessPath;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.LoggerFactory;
import pathexpression.IRegEx;
import sync.pds.solver.EmptyStackWitnessListener;
import sync.pds.solver.SyncPDSSolver;
import sync.pds.solver.WitnessListener;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.NestedWeightedPAutomatons;
import wpds.impl.NormalRule;
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.impl.WeightedPushdownSystem;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;

public abstract class AbstractBoomerangSolver<W extends Weight>
    extends SyncPDSSolver<ControlFlowGraph.Edge, Val, Field, W> {
  private static final org.slf4j.Logger LOGGER =
      LoggerFactory.getLogger(AbstractBoomerangSolver.class);

  protected final ObservableICFG<Statement, Method> icfg;
  protected final ObservableControlFlowGraph cfg;
  protected boolean INTERPROCEDURAL = true;
  protected final Map<
          Entry<INode<Node<ControlFlowGraph.Edge, Val>>, Field>,
          INode<Node<ControlFlowGraph.Edge, Val>>>
      generatedFieldState;
  private Multimap<Method, Transition<Field, INode<Node<ControlFlowGraph.Edge, Val>>>>
      perMethodFieldTransitions = HashMultimap.create();
  private Multimap<Method, MethodBasedFieldTransitionListener<W>>
      perMethodFieldTransitionsListener = HashMultimap.create();
  protected Multimap<
          ControlFlowGraph.Edge, Transition<Field, INode<Node<ControlFlowGraph.Edge, Val>>>>
      perStatementFieldTransitions = HashMultimap.create();
  private Multimap<ControlFlowGraph.Edge, ControlFlowEdgeBasedFieldTransitionListener<W>>
      perStatementFieldTransitionsListener = HashMultimap.create();
  private HashBasedTable<ControlFlowGraph.Edge, Transition<ControlFlowGraph.Edge, INode<Val>>, W>
      perStatementCallTransitions = HashBasedTable.create();
  private Multimap<ControlFlowGraph.Edge, ControlFlowEdgeBasedCallTransitionListener<W>>
      perStatementCallTransitionsListener = HashMultimap.create();
  private Multimap<Method, UnbalancedDataFlow<W>> unbalancedDataFlows = HashMultimap.create();
  private Multimap<Method, UnbalancedDataFlowListener> unbalancedDataFlowListeners =
      HashMultimap.create();
  protected final DataFlowScope dataFlowScope;
  protected final BoomerangOptions options;
  protected final Type type;

  protected final Strategies<W> strategies;

  public AbstractBoomerangSolver(
      ObservableICFG<Statement, Method> icfg,
      ObservableControlFlowGraph cfg,
      Map<Entry<INode<Node<Edge, Val>>, Field>, INode<Node<Edge, Val>>> genField,
      BoomerangOptions options,
      NestedWeightedPAutomatons<ControlFlowGraph.Edge, INode<Val>, W> callSummaries,
      NestedWeightedPAutomatons<Field, INode<Node<ControlFlowGraph.Edge, Val>>, W> fieldSummaries,
      DataFlowScope scope,
      Strategies<W> strategies,
      Type propagationType) {
    super(
        icfg instanceof BackwardsObservableICFG ? false : options.callSummaries(),
        callSummaries,
        options.fieldSummaries(),
        fieldSummaries,
        options.maxCallDepth(),
        options.maxFieldDepth(),
        options.maxUnbalancedCallDepth());
    this.options = options;
    this.icfg = icfg;
    this.cfg = cfg;
    this.dataFlowScope = scope;
    this.strategies = strategies;
    this.type = propagationType;
    this.fieldAutomaton.registerListener(
        (t, w, aut) -> {
          addTransitionToMethod(t.getStart().fact().stmt().getStart().getMethod(), t);
          addTransitionToMethod(t.getTarget().fact().stmt().getStart().getMethod(), t);
          addTransitionToStatement(t.getStart().fact().stmt(), t);
        });
    this.callAutomaton.registerListener(
        (t, w, aut) -> {
          addCallTransitionToStatement(t.getLabel(), t, w);
        });
    this.callAutomaton.registerListener(new UnbalancedListener());
    this.generatedFieldState = genField;
  }

  private class UnbalancedListener
      implements WPAUpdateListener<ControlFlowGraph.Edge, INode<Val>, W> {

    @Override
    public void onWeightAdded(
        Transition<Edge, INode<Val>> t, W w, WeightedPAutomaton<Edge, INode<Val>, W> aut) {
      if (t.getLabel().equals(new Edge(Statement.epsilon(), Statement.epsilon()))) return;
      if (icfg.isExitStmt(
          (AbstractBoomerangSolver.this instanceof ForwardBoomerangSolver
              ? t.getLabel().getTarget()
              : t.getLabel().getStart()))) {
        Statement exitStmt = t.getLabel().getTarget();
        Method callee = exitStmt.getMethod();
        if (callAutomaton.getInitialStates().contains(t.getTarget())) {
          addPotentialUnbalancedFlow(callee, t, w);
        }
      }
    }
  }

  public INode<Node<ControlFlowGraph.Edge, Val>> createQueryNodeField(Query query) {
    return new SingleNode(
        /* TODO Replace by new designated type */ new Node<>(
            query.cfgEdge(), query.asNode().fact().asUnbalanced(query.cfgEdge())));
  }

  public void synchedEmptyStackReachable(
      final Node<Edge, Val> sourceNode, final EmptyStackWitnessListener<Edge, Val> listener) {
    synchedReachable(
        sourceNode,
        new WitnessListener<Edge, Val, Field>() {
          Multimap<Val, Node<Edge, Val>> potentialFieldCandidate = HashMultimap.create();
          Set<Val> potentialCallCandidate = Sets.newHashSet();

          @Override
          public void fieldWitness(Transition<Field, INode<Node<Edge, Val>>> t) {
            if (t.getTarget() instanceof GeneratedState) return;
            if (!t.getLabel().equals(emptyField())) return;
            Node<ControlFlowGraph.Edge, Val> targetFact =
                new Node<>(
                    t.getTarget().fact().stmt(), t.getTarget().fact().fact().asUnbalanced(null));
            if (!potentialFieldCandidate.put(targetFact.fact(), targetFact)) return;
            if (potentialCallCandidate.contains(targetFact.fact())) {
              listener.witnessFound(targetFact);
            }
          }

          @Override
          public void callWitness(Transition<ControlFlowGraph.Edge, INode<Val>> t) {

            Val targetFact = t.getTarget().fact();
            if (targetFact instanceof AllocVal) {
              targetFact = ((AllocVal) targetFact).getDelegate();
              if (!potentialCallCandidate.add(targetFact)) return;
              if (potentialFieldCandidate.containsKey(targetFact)) {
                for (Node<ControlFlowGraph.Edge, Val> w : potentialFieldCandidate.get(targetFact)) {
                  listener.witnessFound(w);
                }
              }
            }
          }
        });
  }

  public void synchedReachable(
      final Node<ControlFlowGraph.Edge, Val> sourceNode,
      final WitnessListener<ControlFlowGraph.Edge, Val, Field> listener) {
    registerListener(
        reachableNode -> {
          if (!reachableNode.equals(sourceNode)) return;
          fieldAutomaton.registerListener(
              (t, w, aut) -> {
                if (t.getStart() instanceof GeneratedState) return;
                if (!t.getStart().fact().equals(sourceNode)) return;
                listener.fieldWitness(t);
              });
          callAutomaton.registerListener(
              (t, w, aut) -> {
                if (t.getStart() instanceof GeneratedState) return;
                if (!t.getStart().fact().equals(sourceNode.fact())) return;
                if (!t.getLabel().equals(sourceNode.stmt())) return;
                if (callAutomaton.isUnbalancedState(t.getTarget())) {
                  listener.callWitness(t);
                }
              });
        });
  }

  protected void addPotentialUnbalancedFlow(
      Method callee, Transition<ControlFlowGraph.Edge, INode<Val>> trans, W weight) {
    if (unbalancedDataFlows.put(callee, new UnbalancedDataFlow<>(callee, trans))) {
      Collection<UnbalancedDataFlowListener> existingListeners =
          Lists.newArrayList(unbalancedDataFlowListeners.get(callee));
      for (UnbalancedDataFlowListener l : existingListeners) {
        propagateUnbalancedToCallSite(l.getCallSiteEdge(), trans);
      }
    }

    if (forceUnbalanced(trans.getTarget(), callAutomaton.getUnbalancedStartOf(trans.getTarget()))) {
      icfg.addCallerListener(
          new CallerListener<Statement, Method>() {

            @Override
            public Method getObservedCallee() {
              return callee;
            }

            @Override
            public void onCallerAdded(Statement n, Method m) {
              if (AbstractBoomerangSolver.this instanceof ForwardBoomerangSolver) {
                cfg.addPredsOfListener(
                    new PredecessorListener(n) {
                      @Override
                      public void getPredecessor(Statement pred) {
                        propagateUnbalancedToCallSite(new Edge(pred, n), trans);
                      }
                    });
              } else if (AbstractBoomerangSolver.this instanceof BackwardBoomerangSolver) {
                cfg.addSuccsOfListener(
                    new SuccessorListener(n) {
                      @Override
                      public void getSuccessor(Statement succ) {
                        propagateUnbalancedToCallSite(new Edge(n, succ), trans);
                      }
                    });
              }
            }
          });
    }
  }

  protected boolean forceUnbalanced(INode<Val> iNode, Collection<INode<Val>> collection) {
    return false;
  }

  public void allowUnbalanced(Method callee, Edge callSiteEdge) {
    if (dataFlowScope.isExcluded(callee)) {
      return;
    }
    UnbalancedDataFlowListener l = new UnbalancedDataFlowListener(callee, callSiteEdge);
    if (unbalancedDataFlowListeners.put(callee, l)) {
      LOGGER.trace(
          "Allowing unbalanced propagation from {} to {} of {}", callee, callSiteEdge, this);
      for (UnbalancedDataFlow<W> e : Lists.newArrayList(unbalancedDataFlows.get(callee))) {
        propagateUnbalancedToCallSite(callSiteEdge, e.getReturningTransition());
      }
    }
  }

  protected abstract void propagateUnbalancedToCallSite(
      Edge callSiteEdge, Transition<ControlFlowGraph.Edge, INode<Val>> transInCallee);

  @Override
  protected boolean preventCallTransitionAdd(
      Transition<ControlFlowGraph.Edge, INode<Val>> t, W weight) {
    return false;
  }

  @Override
  public void addCallRule(final Rule<ControlFlowGraph.Edge, INode<Val>, W> rule) {
    if (rule instanceof NormalRule) {
      if (rule.getL1().equals(rule.getL2()) && rule.getS1().equals(rule.getS2())) return;
    }
    super.addCallRule(rule);
  }

  @Override
  public void addFieldRule(final Rule<Field, INode<Node<ControlFlowGraph.Edge, Val>>, W> rule) {
    if (rule instanceof NormalRule) {
      if (rule.getL1().equals(rule.getL2()) && rule.getS1().equals(rule.getS2())) return;
    }
    super.addFieldRule(rule);
  }

  private void addTransitionToMethod(Method method, Transition<Field, INode<Node<Edge, Val>>> t) {
    if (perMethodFieldTransitions.put(method, t)) {
      for (MethodBasedFieldTransitionListener<W> l :
          Lists.newArrayList(perMethodFieldTransitionsListener.get(method))) {
        l.onAddedTransition(t);
      }
    }
  }

  public void registerFieldTransitionListener(MethodBasedFieldTransitionListener<W> l) {
    if (perMethodFieldTransitionsListener.put(l.getMethod(), l)) {
      for (Transition<Field, INode<Node<ControlFlowGraph.Edge, Val>>> t :
          Lists.newArrayList(perMethodFieldTransitions.get(l.getMethod()))) {
        l.onAddedTransition(t);
      }
    }
  }

  private void addTransitionToStatement(
      ControlFlowGraph.Edge s, Transition<Field, INode<Node<ControlFlowGraph.Edge, Val>>> t) {
    if (perStatementFieldTransitions.put(s, t)) {
      for (ControlFlowEdgeBasedFieldTransitionListener<W> l :
          Lists.newArrayList(perStatementFieldTransitionsListener.get(s))) {
        l.onAddedTransition(t);
      }
    }
  }

  public void registerStatementFieldTransitionListener(
      ControlFlowEdgeBasedFieldTransitionListener<W> l) {
    if (perStatementFieldTransitionsListener.put(l.getCfgEdge(), l)) {
      for (Transition<Field, INode<Node<ControlFlowGraph.Edge, Val>>> t :
          Lists.newArrayList(perStatementFieldTransitions.get(l.getCfgEdge()))) {
        l.onAddedTransition(t);
      }
    }
  }

  private void addCallTransitionToStatement(Edge s, Transition<Edge, INode<Val>> t, W w) {
    W put = perStatementCallTransitions.get(s, t);
    if (put != null) {
      W combineWith = (W) put.combineWith(w);
      if (!combineWith.equals(put)) {
        perStatementCallTransitions.put(s, t, combineWith);
        for (ControlFlowEdgeBasedCallTransitionListener<W> l :
            Lists.newArrayList(perStatementCallTransitionsListener.get(s))) {
          l.onAddedTransition(t, w);
        }
      }
    } else {
      perStatementCallTransitions.put(s, t, w);
      for (ControlFlowEdgeBasedCallTransitionListener<W> l :
          Lists.newArrayList(perStatementCallTransitionsListener.get(s))) {
        l.onAddedTransition(t, w);
      }
    }
  }

  public void registerStatementCallTransitionListener(
      ControlFlowEdgeBasedCallTransitionListener<W> l) {
    if (perStatementCallTransitionsListener.put(l.getControlFlowEdge(), l)) {
      Map<Transition<ControlFlowGraph.Edge, INode<Val>>, W> row =
          perStatementCallTransitions.row(l.getControlFlowEdge());
      for (Entry<Transition<ControlFlowGraph.Edge, INode<Val>>, W> t :
          Lists.newArrayList(row.entrySet())) {
        l.onAddedTransition(t.getKey(), t.getValue());
      }
    }
  }

  public INode<Node<ControlFlowGraph.Edge, Val>> generateFieldState(
      final INode<Node<ControlFlowGraph.Edge, Val>> d, final Field loc) {
    Entry<INode<Node<ControlFlowGraph.Edge, Val>>, Field> e = new AbstractMap.SimpleEntry<>(d, loc);
    if (!generatedFieldState.containsKey(e)) {
      generatedFieldState.put(e, new GeneratedState<>(d, loc));
    }
    return generatedFieldState.get(e);
  }

  protected abstract boolean killFlow(Method method, Statement curr, Val value);

  private boolean isBackward() {
    return this instanceof BackwardBoomerangSolver;
  }

  protected abstract Collection<? extends State> computeReturnFlow(
      Method method, Statement curr, Val value);

  protected void returnFlow(Method method, Node<ControlFlowGraph.Edge, Val> currNode) {
    Val value = currNode.fact();

    Collection<? extends State> outFlow =
        computeReturnFlow(method, currNode.stmt().getTarget(), value);

    for (State s : outFlow) {
      propagate(currNode, s);
    }
  }

  protected abstract Collection<? extends State> getEmptyCalleeFlow(
      Method caller, Edge callSiteEdge, Val value);

  protected abstract Collection<State> computeNormalFlow(Method method, Edge currEdge, Val value);

  @Override
  public Field epsilonField() {
    return Field.epsilon();
  }

  @Override
  public Field emptyField() {
    return Field.empty();
  }

  @Override
  public ControlFlowGraph.Edge epsilonStmt() {
    return new Edge(Statement.epsilon(), Statement.epsilon());
  }

  @Override
  public Field fieldWildCard() {
    return Field.wildcard();
  }

  @Override
  public Field exclusionFieldWildCard(Field exclusion) {
    return Field.exclusionWildcard(exclusion);
  }

  public WeightedPAutomaton<Field, INode<Node<ControlFlowGraph.Edge, Val>>, W> getFieldAutomaton() {
    return fieldAutomaton;
  }

  public WeightedPAutomaton<ControlFlowGraph.Edge, INode<Val>, W> getCallAutomaton() {
    return callAutomaton;
  }

  public WeightedPushdownSystem<ControlFlowGraph.Edge, INode<Val>, W> getCallPDS() {
    return callingPDS;
  }

  public WeightedPushdownSystem<Field, INode<Node<ControlFlowGraph.Edge, Val>>, W> getFieldPDS() {
    return fieldPDS;
  }

  public int getNumberOfRules() {
    return callingPDS.getAllRules().size() + fieldPDS.getAllRules().size();
  }

  @Override
  protected boolean preventFieldTransitionAdd(
      Transition<Field, INode<Node<ControlFlowGraph.Edge, Val>>> t, W weight) {
    if (t.getStart().equals(t.getTarget()) && t.getLabel().equals(Field.empty())) {
      LOGGER.warn("Prevented illegal edge addition of {}", t);
      return true;
    }
    if (!t.getLabel().equals(Field.empty()) || !options.typeCheck()) {
      return false;
    }
    if (t.getTarget() instanceof GeneratedState || t.getStart() instanceof GeneratedState) {
      return false;
    }
    Val target = t.getTarget().fact().fact();
    Val source = t.getStart().fact().fact();

    if (source.isStatic()) {
      return false;
    }

    Type sourceVal = source.getType();
    Type targetVal = (isBackward() ? target.getType() : type);
    if (sourceVal == null) {
      return true;
    }
    if (sourceVal.equals(targetVal)) {
      return false;
    }
    if (!(targetVal.isRefType()) || !(sourceVal.isRefType())) {
      if (options.killNullAtCast() && targetVal.isNullType() && isCastNode(t.getStart().fact())) {
        // A null pointer cannot be cast to any object
        return true;
      }
      return false; // !allocVal.value().getType().equals(varVal.value().getType());
    }
    return sourceVal.doesCastFail(targetVal, target);
  }

  private boolean isCastNode(Node<ControlFlowGraph.Edge, Val> node) {
    boolean isCast = node.stmt().getStart().isCast();
    if (isCast) {
      Val rightOp = node.stmt().getStart().getRightOp();
      if (rightOp.isCast()) {
        if (rightOp.getCastOp().equals(node.fact())) {
          return true;
        }
      }
    }
    return false;
  }

  public Map<RegExAccessPath, W> getResultsAt(final Statement stmt) {
    final Map<RegExAccessPath, W> results = Maps.newHashMap();
    fieldAutomaton.registerListener(
        (t, w, aut) -> {
          if (t.getStart() instanceof GeneratedState) {
            return;
          }
          if (t.getStart().fact().stmt().equals(stmt)) {
            for (INode<Node<ControlFlowGraph.Edge, Val>> initState :
                fieldAutomaton.getInitialStates()) {
              IRegEx<Field> regEx = fieldAutomaton.toRegEx(t.getStart(), initState);

              results.put(new RegExAccessPath(t.getStart().fact().fact(), regEx), w);
            }
          }
        });
    return results;
  }

  public Table<Edge, RegExAccessPath, W> getResults(Method m) {
    final Table<Edge, RegExAccessPath, W> results = HashBasedTable.create();
    LOGGER.debug("Start extracting results from {}", this);
    fieldAutomaton.registerListener(
        (t, w, aut) -> {
          if (t.getStart() instanceof GeneratedState) {
            return;
          }
          if (t.getStart().fact().stmt().getStart().getMethod().equals(m)) {
            for (INode<Node<ControlFlowGraph.Edge, Val>> initState :
                fieldAutomaton.getInitialStates()) {
              IRegEx<Field> regEx = fieldAutomaton.toRegEx(t.getStart(), initState);
              AbstractBoomerangSolver.this.callAutomaton.registerListener(
                  (callT, w1, aut1) -> {
                    if (callT.getStart().fact().equals(t.getStart().fact().fact())
                        && callT.getLabel().equals(t.getStart().fact().stmt())) {
                      results.put(
                          t.getStart().fact().stmt(),
                          new RegExAccessPath(t.getStart().fact().fact(), regEx),
                          w1);
                    }
                  });
            }
          }
        });
    LOGGER.debug("End extracted results from {}", this);
    return results;
  }

  public void debugFieldAutomaton(final Statement stmt) {
    fieldAutomaton.registerListener(
        (t, w, aut) -> {
          if (t.getStart() instanceof GeneratedState) {
            return;
          }
          if (t.getStart().fact().stmt().equals(stmt)) {
            for (INode<Node<ControlFlowGraph.Edge, Val>> initState :
                fieldAutomaton.getInitialStates()) {
              IRegEx<Field> regEx = fieldAutomaton.toRegEx(t.getStart(), initState);
              LOGGER.debug(t.getStart().fact().fact() + " " + regEx);
            }
          }
        });
  }

  public void unregisterAllListeners() {
    this.callAutomaton.unregisterAllListeners();
    this.fieldAutomaton.unregisterAllListeners();
    this.perMethodFieldTransitionsListener.clear();
    this.perStatementCallTransitionsListener.clear();
    this.perStatementFieldTransitionsListener.clear();
    this.unbalancedDataFlowListeners.clear();
    this.unbalancedDataFlows.clear();
    this.callingPDS.unregisterAllListeners();
    this.fieldPDS.unregisterAllListeners();
  }

  private static class UnbalancedDataFlow<W> {

    private final Method callee;
    private Transition<ControlFlowGraph.Edge, INode<Val>> trans;

    public UnbalancedDataFlow(Method callee, Transition<ControlFlowGraph.Edge, INode<Val>> trans) {
      this.callee = callee;
      this.trans = trans;
    }

    public Transition<ControlFlowGraph.Edge, INode<Val>> getReturningTransition() {
      return trans;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((callee == null) ? 0 : callee.hashCode());
      result = prime * result + ((trans == null) ? 0 : trans.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      UnbalancedDataFlow other = (UnbalancedDataFlow) obj;
      if (callee == null) {
        if (other.callee != null) return false;
      } else if (!callee.equals(other.callee)) return false;
      if (trans == null) {
        if (other.trans != null) return false;
      } else if (!trans.equals(other.trans)) return false;
      return true;
    }
  }

  private static class UnbalancedDataFlowListener {
    private Method callee;
    private Edge callSite;

    public UnbalancedDataFlowListener(Method callee, Edge callSite) {
      this.callee = callee;
      this.callSite = callSite;
    }

    public Edge getCallSiteEdge() {
      return callSite;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((callee == null) ? 0 : callee.hashCode());
      result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      UnbalancedDataFlowListener other = (UnbalancedDataFlowListener) obj;
      if (callee == null) {
        if (other.callee != null) return false;
      } else if (!callee.equals(other.callee)) return false;
      if (callSite == null) {
        if (other.callSite != null) return false;
      } else if (!callSite.equals(other.callSite)) return false;
      return true;
    }
  }
}
