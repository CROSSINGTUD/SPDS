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
package ideal;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Field;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.SyncPDSSolver.OnAddedSummaryListener;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.NormalRule;
import wpds.impl.PushRule;
import wpds.impl.Rule;
import wpds.impl.StackListener;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public class IDEALSeedSolver<W extends Weight> {

  private static Logger LOGGER = LoggerFactory.getLogger(IDEALSeedSolver.class);
  private final IDEALAnalysisDefinition<W> analysisDefinition;
  private final ForwardQuery seed;
  private final IDEALWeightFunctions<W> idealWeightFunctions;
  private final W one;
  private final WeightedBoomerang<W> phase1Solver;
  private final WeightedBoomerang<W> phase2Solver;
  private final Stopwatch analysisStopwatch = Stopwatch.createUnstarted();
  private Multimap<Node<Edge, Val>, Edge> affectedStrongUpdateStmt = HashMultimap.create();
  private Set<Node<Edge, Val>> weakUpdates = Sets.newHashSet();
  private int killedRules;

  private final class AddIndirectFlowAtCallSite implements WPAUpdateListener<Edge, INode<Val>, W> {
    private final Edge callSite;
    private final Val returnedFact;

    private AddIndirectFlowAtCallSite(Edge callSite, Val returnedFact) {
      this.callSite = callSite;
      this.returnedFact = returnedFact;
    }

    @Override
    public void onWeightAdded(
        Transition<Edge, INode<Val>> t, W w, WeightedPAutomaton<Edge, INode<Val>, W> aut) {
      if (t.getLabel().equals(callSite)) {
        idealWeightFunctions.addNonKillFlow(new Node<>(callSite, returnedFact));
        idealWeightFunctions.addIndirectFlow(
            new Node<>(callSite, returnedFact), new Node<>(callSite, t.getStart().fact()));
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
      result = prime * result + ((returnedFact == null) ? 0 : returnedFact.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      AddIndirectFlowAtCallSite other = (AddIndirectFlowAtCallSite) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (callSite == null) {
        if (other.callSite != null) return false;
      } else if (!callSite.equals(other.callSite)) return false;
      if (returnedFact == null) {
        if (other.returnedFact != null) return false;
      } else if (!returnedFact.equals(other.returnedFact)) return false;
      return true;
    }

    private IDEALSeedSolver getOuterType() {
      return IDEALSeedSolver.this;
    }
  }

  private final class TriggerBackwardQuery
      extends WPAStateListener<Field, INode<Node<Edge, Val>>, W> {

    private final AbstractBoomerangSolver<W> seedSolver;
    private final WeightedBoomerang<W> boomerang;
    private final Node<Edge, Val> strongUpdateNode;

    private TriggerBackwardQuery(
        AbstractBoomerangSolver<W> seedSolver,
        WeightedBoomerang<W> boomerang,
        Node<Edge, Val> curr) {
      super(new SingleNode<>(curr));
      this.seedSolver = seedSolver;
      this.boomerang = boomerang;
      this.strongUpdateNode = curr;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<Edge, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Edge, Val>>, W> weightedPAutomaton) {
      if (!t.getLabel().equals(Field.empty())) {
        return;
      }

      addAffectedPotentialStrongUpdate(strongUpdateNode, strongUpdateNode.stmt());
      for (Statement u :
          strongUpdateNode
              .stmt()
              .getMethod()
              .getControlFlowGraph()
              .getPredsOf(strongUpdateNode.stmt().getStart())) {
        BackwardQuery query =
            BackwardQuery.make(
                new Edge(u, strongUpdateNode.stmt().getStart()), strongUpdateNode.fact());
        BackwardBoomerangResults<W> queryResults = boomerang.solve(query);
        Set<ForwardQuery> queryAllocationSites = queryResults.getAllocationSites().keySet();
        setWeakUpdateIfNecessary();
        injectAliasesAtStrongUpdates(queryAllocationSites);
        injectAliasesAtStrongUpdatesAtCallStack(queryAllocationSites);
      }
    }

    private void injectAliasesAtStrongUpdatesAtCallStack(Set<ForwardQuery> queryAllocationSites) {
      seedSolver
          .getCallAutomaton()
          .registerListener(
              new StackListener<Edge, INode<Val>, W>(
                  seedSolver.getCallAutomaton(),
                  new SingleNode<>(strongUpdateNode.fact()),
                  strongUpdateNode.stmt()) {
                @Override
                public void anyContext(Edge end) {}

                @Override
                public void stackElement(Edge callSiteEdge) {
                  Statement callSite = callSiteEdge.getStart();
                  boomerang.checkTimeout();
                  addAffectedPotentialStrongUpdate(strongUpdateNode, callSiteEdge);
                  for (ForwardQuery e : queryAllocationSites) {
                    AbstractBoomerangSolver<W> solver = boomerang.getSolvers().get(e);

                    solver.addApplySummaryListener(
                        (OnAddedSummaryListener<Edge, Val>)
                            (summaryCallSite, factInCallee, spInCallee, exitStmt, returnedFact) -> {
                              if (callSiteEdge.equals(summaryCallSite)) {
                                if (callSite.containsInvokeExpr()) {
                                  if (returnedFact.isThisLocal()) {
                                    if (callSite.getInvokeExpr().isInstanceInvokeExpr()) {
                                      solver
                                          .getCallAutomaton()
                                          .registerListener(
                                              new AddIndirectFlowAtCallSite(
                                                  callSiteEdge,
                                                  callSite.getInvokeExpr().getBase()));
                                    }
                                  }
                                  if (returnedFact.isReturnLocal()) {
                                    if (callSite.isAssign()) {
                                      solver
                                          .getCallAutomaton()
                                          .registerListener(
                                              new AddIndirectFlowAtCallSite(
                                                  callSiteEdge, callSite.getLeftOp()));
                                    }
                                  }
                                  for (int i = 0;
                                      i < callSite.getInvokeExpr().getArgs().size();
                                      i++) {
                                    if (returnedFact.isParameterLocal(i)) {
                                      solver
                                          .getCallAutomaton()
                                          .registerListener(
                                              new AddIndirectFlowAtCallSite(
                                                  callSiteEdge,
                                                  callSite.getInvokeExpr().getArg(i)));
                                    }
                                  }
                                }
                              }
                            });
                  }
                }
              });
    }

    private void injectAliasesAtStrongUpdates(Set<ForwardQuery> queryAllocationSites) {
      for (ForwardQuery e : queryAllocationSites) {
        AbstractBoomerangSolver<W> solver = boomerang.getSolvers().get(e);
        solver
            .getCallAutomaton()
            .registerListener(
                (t, w, aut) -> {
                  if (t.getLabel()
                      .equals(
                          strongUpdateNode
                              .stmt()) /* && !t.getStart().fact().equals(curr.fact()) */) {
                    idealWeightFunctions.addNonKillFlow(strongUpdateNode);
                    idealWeightFunctions.addIndirectFlow(
                        strongUpdateNode, new Node<>(strongUpdateNode.stmt(), t.getStart().fact()));
                  }
                });
      }
    }

    private void setWeakUpdateIfNecessary() {
      for (final Entry<ForwardQuery, ForwardBoomerangSolver<W>> e :
          boomerang.getSolvers().entrySet()) {
        e.getValue()
            .synchedEmptyStackReachable(
                strongUpdateNode,
                targetFact -> {
                  if (!e.getKey().asNode().equals(seed.asNode())) {
                    if (!e.getKey().asNode().fact().isNull()) {
                      setWeakUpdate(strongUpdateNode);
                    }
                  }
                });
      }
    }

    @Override
    public void onInTransitionAdded(
        Transition<Field, INode<Node<Edge, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Edge, Val>>, W> weightedPAutomaton) {}
  }

  public enum Phases {
    ObjectFlow,
    ValueFlow
  };

  public IDEALSeedSolver(IDEALAnalysisDefinition<W> analysisDefinition, ForwardQuery seed) {
    this.analysisDefinition = analysisDefinition;
    this.seed = seed;
    this.idealWeightFunctions =
        new IDEALWeightFunctions<W>(
            analysisDefinition.weightFunctions(), analysisDefinition.enableStrongUpdates());
    this.one = analysisDefinition.weightFunctions().getOne();
    this.phase1Solver = createSolver(Phases.ObjectFlow);
    this.phase2Solver = createSolver(Phases.ValueFlow);
  }

  public ForwardBoomerangResults<W> run() {
    LOGGER.debug("Starting Phase 1 of IDEal");
    ForwardBoomerangResults<W> resultPhase1 = runPhase(this.phase1Solver, Phases.ObjectFlow);
    if (resultPhase1.isTimedout()) {
      if (analysisStopwatch.isRunning()) {
        analysisStopwatch.stop();
      }
      throw new IDEALSeedTimeout(this, this.phase1Solver, resultPhase1);
    }
    LOGGER.debug("Starting Phase 2 of IDEal");
    ForwardBoomerangResults<W> resultPhase2 = runPhase(this.phase2Solver, Phases.ValueFlow);
    if (resultPhase2.isTimedout()) {
      if (analysisStopwatch.isRunning()) {
        analysisStopwatch.stop();
      }
      throw new IDEALSeedTimeout(this, this.phase2Solver, resultPhase2);
    }
    LOGGER.debug("Killed Strong Update Rules {}", killedRules);
    return resultPhase2;
  }

  private WeightedBoomerang<W> createSolver(Phases phase) {
    return new WeightedBoomerang<W>(
        analysisDefinition.callGraph(),
        analysisDefinition.getDataFlowScope(),
        analysisDefinition.boomerangOptions()) {

      @Override
      protected WeightFunctions<Edge, Val, Edge, W> getForwardCallWeights(
          ForwardQuery sourceQuery) {
        if (sourceQuery.equals(seed)) return idealWeightFunctions;
        return new OneWeightFunctions<>(one);
      }

      @Override
      protected WeightFunctions<Edge, Val, Field, W> getForwardFieldWeights() {
        return new OneWeightFunctions<>(one);
      }

      @Override
      protected WeightFunctions<Edge, Val, Field, W> getBackwardFieldWeights() {
        return new OneWeightFunctions<>(one);
      }

      @Override
      protected WeightFunctions<Edge, Val, Edge, W> getBackwardCallWeights() {
        return new OneWeightFunctions<>(one);
      }

      @Override
      public boolean preventCallRuleAdd(ForwardQuery sourceQuery, Rule<Edge, INode<Val>, W> rule) {
        if (phase.equals(Phases.ValueFlow) && sourceQuery.equals(seed)) {
          if (preventStrongUpdateFlows(rule)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  protected boolean preventStrongUpdateFlows(Rule<Edge, INode<Val>, W> rule) {
    if (rule.getS1().equals(rule.getS2())) {
      if (idealWeightFunctions.isStrongUpdateStatement(rule.getL2())) {
        if (idealWeightFunctions.isKillFlow(new Node<>(rule.getL2(), rule.getS2().fact()))) {
          killedRules++;
          return true;
        }
      }
    }
    if (rule instanceof PushRule) {
      PushRule<Edge, INode<Val>, W> pushRule = (PushRule<Edge, INode<Val>, W>) rule;
      Edge callSite = pushRule.getCallSite();
      if (idealWeightFunctions.isStrongUpdateStatement(callSite)) {
        if (idealWeightFunctions.isKillFlow(new Node<>(callSite, rule.getS1().fact()))) {
          killedRules++;
          return true;
        }
      }
    }
    return false;
  }

  private ForwardBoomerangResults<W> runPhase(
      final WeightedBoomerang<W> boomerang, final Phases phase) {
    analysisStopwatch.start();
    idealWeightFunctions.setPhase(phase);

    if (phase.equals(Phases.ValueFlow)) {
      registerIndirectFlowListener(boomerang.getSolvers().getOrCreate(seed));
    }

    idealWeightFunctions.registerListener(
        curr -> {
          if (phase.equals(Phases.ValueFlow)) {
            return;
          }
          AbstractBoomerangSolver<W> seedSolver = boomerang.getSolvers().getOrCreate(seed);
          seedSolver
              .getFieldAutomaton()
              .registerListener(new TriggerBackwardQuery(seedSolver, boomerang, curr));
        });
    ForwardBoomerangResults<W> res = boomerang.solve(seed);
    analysisStopwatch.stop();
    if (LOGGER.isDebugEnabled()) {
      boomerang.printAllForwardCallAutomatonFlow();
    }
    boomerang.unregisterAllListeners();
    return res;
  }

  protected void addAffectedPotentialStrongUpdate(Node<Edge, Val> strongUpdateNode, Edge stmt) {
    if (affectedStrongUpdateStmt.put(strongUpdateNode, stmt)) {
      idealWeightFunctions.potentialStrongUpdate(stmt);
      if (weakUpdates.contains(strongUpdateNode)) {
        idealWeightFunctions.weakUpdate(stmt);
      }
    }
  }

  private void setWeakUpdate(Node<Edge, Val> curr) {
    LOGGER.debug("Weak update @ {}", curr);
    if (weakUpdates.add(curr)) {
      for (Edge s : Lists.newArrayList(affectedStrongUpdateStmt.get(curr))) {
        idealWeightFunctions.weakUpdate(s);
      }
    }
  }

  private void registerIndirectFlowListener(AbstractBoomerangSolver<W> solver) {
    WeightedPAutomaton<Edge, INode<Val>, W> callAutomaton = solver.getCallAutomaton();
    callAutomaton.registerListener(
        (t, w, aut) -> {
          if (t.getStart() instanceof GeneratedState) return;
          Node<Edge, Val> source = new Node<>(t.getLabel(), t.getStart().fact());
          Collection<Node<Edge, Val>> indirectFlows = idealWeightFunctions.getAliasesFor(source);
          for (Node<Edge, Val> indirectFlow : indirectFlows) {
            solver.addCallRule(
                new NormalRule<>(
                    new SingleNode<>(source.fact()),
                    source.stmt(),
                    new SingleNode<>(indirectFlow.fact()),
                    indirectFlow.stmt(),
                    one));
            solver.addFieldRule(
                new NormalRule<>(
                    solver.asFieldFact(source),
                    solver.fieldWildCard(),
                    solver.asFieldFact(indirectFlow),
                    solver.fieldWildCard(),
                    one));
          }
        });
  }

  public WeightedBoomerang<W> getPhase1Solver() {
    return phase1Solver;
  }

  public WeightedBoomerang<W> getPhase2Solver() {
    return phase2Solver;
  }

  public Stopwatch getAnalysisStopwatch() {
    return analysisStopwatch;
  }

  public Query getSeed() {
    return seed;
  }
}
