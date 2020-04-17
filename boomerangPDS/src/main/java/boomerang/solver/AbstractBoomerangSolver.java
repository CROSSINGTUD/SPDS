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
import boomerang.scene.AllocVal;
import boomerang.scene.CallSiteStatement;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Field;
import boomerang.scene.Method;
import boomerang.scene.ReturnSiteStatement;
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
    extends SyncPDSSolver<Statement, Val, Field, W> {
  private static final org.slf4j.Logger LOGGER =
      LoggerFactory.getLogger(AbstractBoomerangSolver.class);

  protected final ObservableICFG<Statement, Method> icfg;
  protected final ObservableControlFlowGraph cfg;
  protected boolean INTERPROCEDURAL = true;
  protected final Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>>
      generatedFieldState;
  private Multimap<Method, Transition<Field, INode<Node<Statement, Val>>>>
      perMethodFieldTransitions = HashMultimap.create();
  private Multimap<Method, MethodBasedFieldTransitionListener<W>>
      perMethodFieldTransitionsListener = HashMultimap.create();
  protected Multimap<Statement, Transition<Field, INode<Node<Statement, Val>>>>
      perStatementFieldTransitions = HashMultimap.create();
  private Multimap<Statement, StatementBasedFieldTransitionListener<W>>
      perStatementFieldTransitionsListener = HashMultimap.create();
  private HashBasedTable<Statement, Transition<Statement, INode<Val>>, W>
      perStatementCallTransitions = HashBasedTable.create();
  private Multimap<Statement, StatementBasedCallTransitionListener<W>>
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
      Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> genField,
      BoomerangOptions options,
      NestedWeightedPAutomatons<Statement, INode<Val>, W> callSummaries,
      NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> fieldSummaries,
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
          addTransitionToMethod(t.getStart().fact().stmt().getMethod(), t);
          addTransitionToMethod(t.getTarget().fact().stmt().getMethod(), t);
          addTransitionToStatement(t.getStart().fact().stmt(), t);
        });
    this.callAutomaton.registerListener(
        (t, w, aut) -> {
          addCallTransitionToStatement(t.getLabel(), t, w);
          if (t.getStart() instanceof GeneratedState) {

            if (isBackward() && !(t.getLabel() instanceof CallSiteStatement)) {
              throw new RuntimeException(
                  "The backward analysis shall only have CallSiteStatements on pushs");
            } else if (!isBackward() && !(t.getLabel() instanceof ReturnSiteStatement)) {
              throw new RuntimeException(
                  "The forward analysis shall only have ReturnSiteStatement on pushs");
            }
          }
        });
    this.callAutomaton.registerListener(new UnbalancedListener());

    // TODO recap, I assume we can implement this more easily.
    this.generatedFieldState = genField;
  }

  private class UnbalancedListener implements WPAUpdateListener<Statement, INode<Val>, W> {

    @Override
    public void onWeightAdded(
        Transition<Statement, INode<Val>> t,
        W w,
        WeightedPAutomaton<Statement, INode<Val>, W> aut) {
      if (t.getLabel().equals(Statement.epsilon())) return;
      if (icfg.isExitStmt(t.getLabel())) {
        Statement exitStmt = t.getLabel();
        Method callee = exitStmt.getMethod();
        if (callAutomaton.getInitialStates().contains(t.getTarget())) {
          addPotentialUnbalancedFlow(callee, t, w);
        }
      }
    }
  }

  public INode<Node<Statement, Val>> createQueryNodeField(Query query) {
    return new SingleNode(
        /* TODO Replace by new designated type */ new Node<>(
            query.stmt(), query.asNode().fact().asUnbalanced(query.stmt())));
  }

  public void synchedEmptyStackReachable(
      final Node<Statement, Val> sourceNode,
      final EmptyStackWitnessListener<Statement, Val> listener) {
    synchedReachable(
        sourceNode,
        new WitnessListener<Statement, Val, Field>() {
          Multimap<Val, Node<Statement, Val>> potentialFieldCandidate = HashMultimap.create();
          Set<Val> potentialCallCandidate = Sets.newHashSet();

          @Override
          public void fieldWitness(Transition<Field, INode<Node<Statement, Val>>> t) {
            if (t.getTarget() instanceof GeneratedState) return;
            if (!t.getLabel().equals(emptyField())) return;
            Node<Statement, Val> targetFact =
                new Node<>(
                    t.getTarget().fact().stmt(), t.getTarget().fact().fact().asUnbalanced(null));
            if (!potentialFieldCandidate.put(targetFact.fact(), targetFact)) return;
            if (potentialCallCandidate.contains(targetFact.fact())) {
              listener.witnessFound(targetFact);
            }
          }

          @Override
          public void callWitness(Transition<Statement, INode<Val>> t) {

            Val targetFact = t.getTarget().fact();
            if (targetFact instanceof AllocVal) {
              targetFact = ((AllocVal) targetFact).getDelegate();
              if (!potentialCallCandidate.add(targetFact)) return;
              if (potentialFieldCandidate.containsKey(targetFact)) {
                for (Node<Statement, Val> w : potentialFieldCandidate.get(targetFact)) {
                  listener.witnessFound(w);
                }
              }
            }
          }
        });
  }

  public void synchedReachable(
      final Node<Statement, Val> sourceNode,
      final WitnessListener<Statement, Val, Field> listener) {
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
      Method callee, Transition<Statement, INode<Val>> trans, W weight) {
    if (unbalancedDataFlows.put(callee, new UnbalancedDataFlow<>(callee, trans))) {
      Collection<UnbalancedDataFlowListener> existingListeners =
          Lists.newArrayList(unbalancedDataFlowListeners.get(callee));
      for (UnbalancedDataFlowListener l : existingListeners) {
        propagateUnbalancedToCallSite(l.getCallSite(), trans);
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
              propagateUnbalancedToCallSite((CallSiteStatement) n, trans);
            }
          });
    }
  }

  protected boolean forceUnbalanced(INode<Val> iNode, Collection<INode<Val>> collection) {
    return false;
  }

  public void allowUnbalanced(Method callee, CallSiteStatement callSite) {
    if (dataFlowScope.isExcluded(callee)) {
      return;
    }
    UnbalancedDataFlowListener l = new UnbalancedDataFlowListener(callee, callSite);
    if (unbalancedDataFlowListeners.put(callee, l)) {
      LOGGER.trace("Allowing unbalanced propagation from {} to {} of {}", callee, callSite, this);
      for (UnbalancedDataFlow<W> e : Lists.newArrayList(unbalancedDataFlows.get(callee))) {
        propagateUnbalancedToCallSite(callSite, e.getReturningTransition());
      }
    }
  }

  protected abstract void propagateUnbalancedToCallSite(
      CallSiteStatement callSite, Transition<Statement, INode<Val>> transInCallee);

  @Override
  protected boolean preventCallTransitionAdd(Transition<Statement, INode<Val>> t, W weight) {
    return false;
  }

  @Override
  public void addCallRule(final Rule<Statement, INode<Val>, W> rule) {
    if (rule instanceof NormalRule) {
      if (rule.getL1().equals(rule.getL2()) && rule.getS1().equals(rule.getS2())) return;
    }
    super.addCallRule(rule);
  }

  @Override
  public void addFieldRule(final Rule<Field, INode<Node<Statement, Val>>, W> rule) {
    if (rule instanceof NormalRule) {
      if (rule.getL1().equals(rule.getL2()) && rule.getS1().equals(rule.getS2())) return;
    }
    super.addFieldRule(rule);
  }

  private void addTransitionToMethod(
      Method method, Transition<Field, INode<Node<Statement, Val>>> t) {
    if (perMethodFieldTransitions.put(method, t)) {
      for (MethodBasedFieldTransitionListener<W> l :
          Lists.newArrayList(perMethodFieldTransitionsListener.get(method))) {
        l.onAddedTransition(t);
      }
    }
  }

  public void registerFieldTransitionListener(MethodBasedFieldTransitionListener<W> l) {
    if (perMethodFieldTransitionsListener.put(l.getMethod(), l)) {
      for (Transition<Field, INode<Node<Statement, Val>>> t :
          Lists.newArrayList(perMethodFieldTransitions.get(l.getMethod()))) {
        l.onAddedTransition(t);
      }
    }
  }

  private void addTransitionToStatement(
      Statement s, Transition<Field, INode<Node<Statement, Val>>> t) {
    if (perStatementFieldTransitions.put(s, t)) {
      for (StatementBasedFieldTransitionListener<W> l :
          Lists.newArrayList(perStatementFieldTransitionsListener.get(s))) {
        l.onAddedTransition(t);
      }
    }
  }

  public void registerStatementFieldTransitionListener(StatementBasedFieldTransitionListener<W> l) {
    if (perStatementFieldTransitionsListener.put(l.getStmt(), l)) {
      for (Transition<Field, INode<Node<Statement, Val>>> t :
          Lists.newArrayList(perStatementFieldTransitions.get(l.getStmt()))) {
        l.onAddedTransition(t);
      }
    }
  }

  private void addCallTransitionToStatement(Statement s, Transition<Statement, INode<Val>> t, W w) {
    W put = perStatementCallTransitions.get(s, t);
    if (put != null) {
      W combineWith = (W) put.combineWith(w);
      if (!combineWith.equals(put)) {
        perStatementCallTransitions.put(s, t, combineWith);
        for (StatementBasedCallTransitionListener<W> l :
            Lists.newArrayList(perStatementCallTransitionsListener.get(s))) {
          l.onAddedTransition(t, w);
        }
      }
    } else {
      perStatementCallTransitions.put(s, t, w);
      for (StatementBasedCallTransitionListener<W> l :
          Lists.newArrayList(perStatementCallTransitionsListener.get(s))) {
        l.onAddedTransition(t, w);
      }
    }
  }

  public void registerStatementCallTransitionListener(StatementBasedCallTransitionListener<W> l) {
    if (perStatementCallTransitionsListener.put(l.getStmt(), l)) {
      Map<Transition<Statement, INode<Val>>, W> row = perStatementCallTransitions.row(l.getStmt());
      for (Entry<Transition<Statement, INode<Val>>, W> t : Lists.newArrayList(row.entrySet())) {
        l.onAddedTransition(t.getKey(), t.getValue());
      }
    }
  }

  public INode<Node<Statement, Val>> generateFieldState(
      final INode<Node<Statement, Val>> d, final Field loc) {
    Entry<INode<Node<Statement, Val>>, Field> e = new AbstractMap.SimpleEntry<>(d, loc);
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

  protected void returnFlow(Method method, Node<Statement, Val> currNode) {
    Val value = currNode.fact();

    // Apply summaries here for ,
    //              (CallSiteStatement) callSite,
    //              ((CallSiteStatement) callSite).getReturnSiteStatement()
    Collection<? extends State> outFlow = computeReturnFlow(method, currNode.stmt(), value);

    for (State s : outFlow) {
      propagate(currNode, s);
    }
  }

  protected abstract Collection<? extends State> getEmptyCalleeFlow(
      Method caller, Statement callSite, Val value, Statement returnSite);

  protected abstract Collection<State> computeNormalFlow(
      Method method, Statement curr, Val value, Statement succ);

  @Override
  public Field epsilonField() {
    return Field.epsilon();
  }

  @Override
  public Field emptyField() {
    return Field.empty();
  }

  @Override
  public Statement epsilonStmt() {
    return Statement.epsilon();
  }

  @Override
  public Field fieldWildCard() {
    return Field.wildcard();
  }

  @Override
  public Field exclusionFieldWildCard(Field exclusion) {
    return Field.exclusionWildcard(exclusion);
  }

  public WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> getFieldAutomaton() {
    return fieldAutomaton;
  }

  public WeightedPAutomaton<Statement, INode<Val>, W> getCallAutomaton() {
    return callAutomaton;
  }

  public WeightedPushdownSystem<Statement, INode<Val>, W> getCallPDS() {
    return callingPDS;
  }

  public WeightedPushdownSystem<Field, INode<Node<Statement, Val>>, W> getFieldPDS() {
    return fieldPDS;
  }

  public int getNumberOfRules() {
    return callingPDS.getAllRules().size() + fieldPDS.getAllRules().size();
  }

  @Override
  protected boolean preventFieldTransitionAdd(
      Transition<Field, INode<Node<Statement, Val>>> t, W weight) {
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

  private boolean isCastNode(Node<Statement, Val> node) {
    boolean isCast = node.stmt().isCast();
    if (isCast) {
      Val rightOp = node.stmt().getRightOp();
      if (rightOp.isCast()) {
        if (rightOp.getCastOp().equals(node.fact())) {
          return true;
        }
      }
    }
    return false;
  }

  private final class ReturnFlowCallerListener implements CallerListener<Statement, Method> {
    private final Statement curr;
    private final Method method;
    private final Val value;
    private final Node<Statement, Val> currNode;

    private ReturnFlowCallerListener(
        Statement curr, Method method, Val value, Node<Statement, Val> currNode) {
      this.curr = curr;
      this.method = method;
      this.value = value;
      this.currNode = currNode;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((curr == null) ? 0 : curr.hashCode());
      result = prime * result + ((currNode == null) ? 0 : currNode.hashCode());
      result = prime * result + ((method == null) ? 0 : method.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ReturnFlowCallerListener other = (ReturnFlowCallerListener) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (curr == null) {
        if (other.curr != null) return false;
      } else if (!curr.equals(other.curr)) return false;
      if (currNode == null) {
        if (other.currNode != null) return false;
      } else if (!currNode.equals(other.currNode)) return false;
      if (method == null) {
        if (other.method != null) return false;
      } else if (!method.equals(other.method)) return false;
      if (value == null) {
        if (other.value != null) return false;
      } else if (!value.equals(other.value)) return false;
      return true;
    }

    @Override
    public void onCallerAdded(Statement callSite, Method m) {
      if (!callSite.containsInvokeExpr()) {
        return;
      }
      LOGGER.trace(
          "Balanced return-flow for {} at call site for {}", currNode.fact(), callSite, this);
    }

    @Override
    public Method getObservedCallee() {
      return method;
    }

    private AbstractBoomerangSolver getOuterType() {
      return AbstractBoomerangSolver.this;
    }
  }

  public Map<RegExAccessPath, W> getResultsAt(final Statement stmt) {
    final Map<RegExAccessPath, W> results = Maps.newHashMap();
    fieldAutomaton.registerListener(
        (t, w, aut) -> {
          if (t.getStart() instanceof GeneratedState) {
            return;
          }
          if (t.getStart().fact().stmt().equals(stmt)) {
            for (INode<Node<Statement, Val>> initState : fieldAutomaton.getInitialStates()) {
              IRegEx<Field> regEx = fieldAutomaton.toRegEx(t.getStart(), initState);

              results.put(new RegExAccessPath(t.getStart().fact().fact(), regEx), w);
            }
          }
        });
    return results;
  }

  public Table<Statement, RegExAccessPath, W> getResults(Method m) {
    final Table<Statement, RegExAccessPath, W> results = HashBasedTable.create();
    LOGGER.debug("Start extracting results from {}", this);
    fieldAutomaton.registerListener(
        (t, w, aut) -> {
          if (t.getStart() instanceof GeneratedState) {
            return;
          }
          if (t.getStart().fact().stmt().getMethod().equals(m)) {

            for (INode<Node<Statement, Val>> initState : fieldAutomaton.getInitialStates()) {
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
        new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {

          @Override
          public void onWeightAdded(
              Transition<Field, INode<Node<Statement, Val>>> t,
              W w,
              WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
            if (t.getStart() instanceof GeneratedState) {
              return;
            }
            if (t.getStart().fact().stmt().equals(stmt)) {
              for (INode<Node<Statement, Val>> initState : fieldAutomaton.getInitialStates()) {
                IRegEx<Field> regEx = fieldAutomaton.toRegEx(t.getStart(), initState);
                LOGGER.debug(t.getStart().fact().fact() + " " + regEx);
              }
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
    private Transition<Statement, INode<Val>> trans;

    public UnbalancedDataFlow(Method callee, Transition<Statement, INode<Val>> trans) {
      this.callee = callee;
      this.trans = trans;
    }

    public Transition<Statement, INode<Val>> getReturningTransition() {
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
    private CallSiteStatement callSite;

    public UnbalancedDataFlowListener(Method callee, CallSiteStatement callSite) {
      this.callee = callee;
      this.callSite = callSite;
    }

    public CallSiteStatement getCallSite() {
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
