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
package sync.pds.solver;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.LoggerFactory;
import sync.pds.solver.nodes.ExclusionNode;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.NestedAutomatonListener;
import wpds.impl.NestedWeightedPAutomatons;
import wpds.impl.NormalRule;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.impl.WeightedPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public abstract class SyncPDSSolver<
    Stmt extends Location, Fact, Field extends Location, W extends Weight> {

  public enum PDSSystem {
    FIELDS,
    CALLS
  }

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SyncPDSSolver.class);
  private static final boolean FieldSensitive = true;
  private static final boolean ContextSensitive = true;
  protected final WeightedPushdownSystem<Stmt, INode<Fact>, W> callingPDS =
      new WeightedPushdownSystem<Stmt, INode<Fact>, W>() {
        public String toString() {
          return "Call " + SyncPDSSolver.this.toString();
        };
      };
  protected final WeightedPushdownSystem<Field, INode<Node<Stmt, Fact>>, W> fieldPDS =
      new WeightedPushdownSystem<Field, INode<Node<Stmt, Fact>>, W>() {
        public String toString() {
          return "Field " + SyncPDSSolver.this.toString();
        };
      };
  private final Set<Node<Stmt, Fact>> reachedStates = Sets.newHashSet();
  private final Set<Node<Stmt, Fact>> callingContextReachable = Sets.newHashSet();
  private final Set<Node<Stmt, Fact>> fieldContextReachable = Sets.newHashSet();
  private final Set<SyncPDSUpdateListener<Stmt, Fact>> updateListeners = Sets.newHashSet();
  private final Multimap<Node<Stmt, Fact>, SyncStatePDSUpdateListener<Stmt, Fact>>
      reachedStateUpdateListeners = HashMultimap.create();
  protected final WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> fieldAutomaton;
  protected final WeightedPAutomaton<Stmt, INode<Fact>, W> callAutomaton;

  protected boolean preventFieldTransitionAdd(
      Transition<Field, INode<Node<Stmt, Fact>>> trans, W weight) {
    return false;
  }

  protected boolean preventCallTransitionAdd(Transition<Stmt, INode<Fact>> trans, W weight) {
    return false;
  }

  public SyncPDSSolver(
      final boolean useCallSummaries,
      NestedWeightedPAutomatons<Stmt, INode<Fact>, W> callSummaries,
      final boolean useFieldSummaries,
      NestedWeightedPAutomatons<Field, INode<Node<Stmt, Fact>>, W> fieldSummaries,
      int maxCallDepth,
      int maxFieldDepth,
      int maxUnbalancedCallDepth) {
    fieldAutomaton =
        new WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W>() {
          @Override
          public INode<Node<Stmt, Fact>> createState(INode<Node<Stmt, Fact>> d, Field loc) {
            if (loc.equals(emptyField())) return d;
            return generateFieldState(d, loc);
          }

          @Override
          public Field epsilon() {
            return epsilonField();
          }

          @Override
          public boolean nested() {
            return useFieldSummaries;
          };

          @Override
          public W getOne() {
            return getFieldWeights().getOne();
          }

          @Override
          public int getMaxDepth() {
            return maxFieldDepth;
          }

          public boolean addWeightForTransition(
              Transition<Field, INode<Node<Stmt, Fact>>> trans, W weight) {
            if (preventFieldTransitionAdd(trans, weight)) return false;
            logger.trace("Adding field transition {} with weight {}", trans, weight);
            return super.addWeightForTransition(trans, weight);
          };

          @Override
          public boolean isGeneratedState(INode<Node<Stmt, Fact>> d) {
            return d instanceof GeneratedState;
          }
        };

    callAutomaton =
        new WeightedPAutomaton<Stmt, INode<Fact>, W>() {
          @Override
          public INode<Fact> createState(INode<Fact> d, Stmt loc) {
            return generateCallState(d, loc);
          }

          @Override
          public Stmt epsilon() {
            return epsilonStmt();
          }

          @Override
          public boolean nested() {
            return useCallSummaries;
          };

          @Override
          public W getOne() {
            return getCallWeights().getOne();
          }

          public boolean addWeightForTransition(Transition<Stmt, INode<Fact>> trans, W weight) {
            if (preventCallTransitionAdd(trans, weight)) return false;
            logger.trace("Adding call transition {} with weight {}", trans, weight);
            return super.addWeightForTransition(trans, weight);
          };

          @Override
          public boolean isGeneratedState(INode<Fact> d) {
            return d instanceof GeneratedState;
          }

          @Override
          public int getMaxDepth() {
            return maxCallDepth;
          }

          @Override
          public int getMaxUnbalancedDepth() {
            return maxUnbalancedCallDepth;
          }
        };

    callAutomaton.registerListener(new CallAutomatonListener());
    fieldAutomaton.registerListener(new FieldUpdateListener());
    if (callAutomaton.nested())
      callAutomaton.registerNestedAutomatonListener(new CallSummaryListener());
    // if(fieldAutomaton.nested())
    // fieldAutomaton.registerNestedAutomatonListener(new FieldSummaryListener());
    callingPDS.poststar(callAutomaton, callSummaries);
    fieldPDS.poststar(fieldAutomaton, fieldSummaries);
  }

  private class FieldSummaryListener
      implements NestedAutomatonListener<Field, INode<Node<Stmt, Fact>>, W> {
    @Override
    public void nestedAutomaton(
        final WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> parent,
        final WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> child) {
      for (INode<Node<Stmt, Fact>> s : child.getInitialStates()) {
        child.registerListener(new FieldAddEpsilonToInitialStateListener(s, parent));
      }
    }
  }

  private class FieldAddEpsilonToInitialStateListener
      extends WPAStateListener<Field, INode<Node<Stmt, Fact>>, W> {

    private WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> parent;

    public FieldAddEpsilonToInitialStateListener(
        INode<Node<Stmt, Fact>> state,
        WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> parent) {
      super(state);
      this.parent = parent;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<Stmt, Fact>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> weightedPAutomaton) {}

    @Override
    public void onInTransitionAdded(
        final Transition<Field, INode<Node<Stmt, Fact>>> nestedT,
        W w,
        WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> weightedPAutomaton) {
      if (nestedT.getLabel().equals(fieldAutomaton.epsilon())) {
        parent.registerListener(
            new FieldOnOutTransitionAddToStateListener(this.getState(), nestedT));
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((parent == null) ? 0 : parent.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      FieldAddEpsilonToInitialStateListener other = (FieldAddEpsilonToInitialStateListener) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (parent == null) {
        if (other.parent != null) return false;
      } else if (!parent.equals(other.parent)) return false;
      return true;
    }

    private SyncPDSSolver getOuterType() {
      return SyncPDSSolver.this;
    }
  }

  private class FieldOnOutTransitionAddToStateListener
      extends WPAStateListener<Field, INode<Node<Stmt, Fact>>, W> {
    private Transition<Field, INode<Node<Stmt, Fact>>> nestedT;

    public FieldOnOutTransitionAddToStateListener(
        INode<Node<Stmt, Fact>> state, Transition<Field, INode<Node<Stmt, Fact>>> nestedT) {
      super(state);
      this.nestedT = nestedT;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<Stmt, Fact>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> weightedPAutomaton) {
      setFieldContextReachable(nestedT.getStart().fact());
    }

    @Override
    public void onInTransitionAdded(
        Transition<Field, INode<Node<Stmt, Fact>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> weightedPAutomaton) {}

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((nestedT == null) ? 0 : nestedT.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      FieldOnOutTransitionAddToStateListener other = (FieldOnOutTransitionAddToStateListener) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (nestedT == null) {
        if (other.nestedT != null) return false;
      } else if (!nestedT.equals(other.nestedT)) return false;
      return true;
    }

    private SyncPDSSolver getOuterType() {
      return SyncPDSSolver.this;
    }
  }

  private class CallSummaryListener implements NestedAutomatonListener<Stmt, INode<Fact>, W> {
    @Override
    public void nestedAutomaton(
        final WeightedPAutomaton<Stmt, INode<Fact>, W> parent,
        final WeightedPAutomaton<Stmt, INode<Fact>, W> child) {
      for (INode<Fact> s : child.getInitialStates()) {
        child.registerListener(new AddEpsilonToInitialStateListener(s, parent));
      }
    }
  }

  private class AddEpsilonToInitialStateListener extends WPAStateListener<Stmt, INode<Fact>, W> {

    private WeightedPAutomaton<Stmt, INode<Fact>, W> parent;

    public AddEpsilonToInitialStateListener(
        INode<Fact> state, WeightedPAutomaton<Stmt, INode<Fact>, W> parent) {
      super(state);
      this.parent = parent;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Stmt, INode<Fact>> t,
        W w,
        WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {}

    @Override
    public void onInTransitionAdded(
        final Transition<Stmt, INode<Fact>> nestedT,
        W w,
        WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {
      if (nestedT.getLabel().equals(callAutomaton.epsilon())) {
        parent.registerListener(new OnOutTransitionAddToStateListener(this.getState(), nestedT));
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((parent == null) ? 0 : parent.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      AddEpsilonToInitialStateListener other = (AddEpsilonToInitialStateListener) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (parent == null) {
        if (other.parent != null) return false;
      } else if (!parent.equals(other.parent)) return false;
      return true;
    }

    private SyncPDSSolver getOuterType() {
      return SyncPDSSolver.this;
    }
  }

  private class OnOutTransitionAddToStateListener extends WPAStateListener<Stmt, INode<Fact>, W> {
    private Transition<Stmt, INode<Fact>> nestedT;

    public OnOutTransitionAddToStateListener(
        INode<Fact> state, Transition<Stmt, INode<Fact>> nestedT) {
      super(state);
      this.nestedT = nestedT;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Stmt, INode<Fact>> t,
        W w,
        WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {
      Node<Stmt, Fact> returningNode =
          new Node<Stmt, Fact>(t.getLabel(), nestedT.getStart().fact());
      setCallingContextReachable(returningNode);
    }

    @Override
    public void onInTransitionAdded(
        Transition<Stmt, INode<Fact>> t,
        W w,
        WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {}

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((nestedT == null) ? 0 : nestedT.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      OnOutTransitionAddToStateListener other = (OnOutTransitionAddToStateListener) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (nestedT == null) {
        if (other.nestedT != null) return false;
      } else if (!nestedT.equals(other.nestedT)) return false;
      return true;
    }

    private SyncPDSSolver getOuterType() {
      return SyncPDSSolver.this;
    }
  }

  private class CallAutomatonListener implements WPAUpdateListener<Stmt, INode<Fact>, W> {

    @Override
    public void onWeightAdded(
        Transition<Stmt, INode<Fact>> t, W w, WeightedPAutomaton<Stmt, INode<Fact>, W> aut) {
      if (!(t.getStart() instanceof GeneratedState)
          && !t.getLabel().equals(callAutomaton.epsilon())) {
        Node<Stmt, Fact> node = new Node<Stmt, Fact>(t.getLabel(), t.getStart().fact());
        setCallingContextReachable(node);
      }
    }
  }

  public void solve(
      Node<Stmt, Fact> curr,
      Field field,
      INode<Node<Stmt, Fact>> fieldTarget,
      Stmt stmt,
      INode<Fact> callTarget,
      W weight) {
    fieldAutomaton.addInitialState(fieldTarget);
    callAutomaton.addInitialState(callTarget);
    INode<Node<Stmt, Fact>> start = asFieldFact(curr);
    if (!field.equals(emptyField())) {
      INode<Node<Stmt, Fact>> generateFieldState = generateFieldState(start, field);
      Transition<Field, INode<Node<Stmt, Fact>>> fieldTrans =
          new Transition<>(start, field, generateFieldState);
      fieldAutomaton.addTransition(fieldTrans);
      Transition<Field, INode<Node<Stmt, Fact>>> fieldTransToInitial =
          new Transition<>(generateFieldState, emptyField(), fieldTarget);
      fieldAutomaton.addTransition(fieldTransToInitial);
    } else {
      Transition<Field, INode<Node<Stmt, Fact>>> fieldTrans =
          new Transition<>(start, emptyField(), fieldTarget);
      fieldAutomaton.addTransition(fieldTrans);
    }
    Transition<Stmt, INode<Fact>> callTrans =
        new Transition<>(wrap(curr.fact()), curr.stmt(), callTarget);
    callAutomaton.addWeightForTransition(callTrans, weight);
    processNode(curr);
  }

  public void solve(
      Node<Stmt, Fact> curr,
      Field field,
      INode<Node<Stmt, Fact>> fieldTarget,
      Stmt stmt,
      INode<Fact> callTarget) {
    solve(curr, field, fieldTarget, stmt, callTarget, getCallWeights().getOne());
  }

  public void processNode(Node<Stmt, Fact> curr) {
    if (!addReachableState(curr)) return;
    computeSuccessor(curr);
  }

  public void propagate(Node<Stmt, Fact> curr, State s) {
    if (s instanceof Node) {
      Node<Stmt, Fact> succ = (Node<Stmt, Fact>) s;
      if (succ instanceof PushNode) {
        PushNode<Stmt, Fact, Location> pushNode = (PushNode<Stmt, Fact, Location>) succ;
        PDSSystem system = pushNode.system();
        Location location = pushNode.location();
        processPush(curr, location, pushNode, system);
      } else {
        processNormal(curr, succ);
      }
    } else if (s instanceof PopNode) {
      PopNode<Fact> popNode = (PopNode<Fact>) s;
      processPop(curr, popNode);
    }
  }

  private boolean addReachableState(Node<Stmt, Fact> curr) {
    if (reachedStates.contains(curr)) return false;
    reachedStates.add(curr);
    for (SyncPDSUpdateListener<Stmt, Fact> l : Lists.newLinkedList(updateListeners)) {
      l.onReachableNodeAdded(curr);
    }
    for (SyncStatePDSUpdateListener<Stmt, Fact> l :
        Lists.newLinkedList(reachedStateUpdateListeners.get(curr))) {
      l.reachable();
    }
    return true;
  }

  public void processNormal(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ) {
    addNormalFieldFlow(curr, succ);
    addNormalCallFlow(curr, succ);
  }

  public void addNormalCallFlow(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ) {
    addCallRule(
        new NormalRule<>(
            wrap(curr.fact()),
            curr.stmt(),
            wrap(succ.fact()),
            succ.stmt(),
            getCallWeights().normal(curr, succ)));
  }

  public void addNormalFieldFlow(final Node<Stmt, Fact> curr, final Node<Stmt, Fact> succ) {
    if (succ instanceof ExclusionNode) {
      ExclusionNode<Stmt, Fact, Field> exNode = (ExclusionNode) succ;
      addFieldRule(
          new NormalRule<>(
              asFieldFact(curr),
              fieldWildCard(),
              asFieldFact(succ),
              exclusionFieldWildCard(exNode.exclusion()),
              getFieldWeights().normal(curr, succ)));
      return;
    }
    addFieldRule(
        new NormalRule<>(
            asFieldFact(curr),
            fieldWildCard(),
            asFieldFact(succ),
            fieldWildCard(),
            getFieldWeights().normal(curr, succ)));
  }

  public INode<Node<Stmt, Fact>> asFieldFact(Node<Stmt, Fact> node) {
    return new SingleNode<>(new Node<>(node.stmt(), node.fact()));
  }

  public void processPop(Node<Stmt, Fact> curr, PopNode popNode) {
    PDSSystem system = popNode.system();
    Object location = popNode.location();
    if (system.equals(PDSSystem.FIELDS)) {
      NodeWithLocation<Stmt, Fact, Field> node = (NodeWithLocation) location;
      if (FieldSensitive) {
        addFieldRule(
            new PopRule<>(
                asFieldFact(curr),
                node.location(),
                asFieldFact(node.fact()),
                getFieldWeights().pop(curr)));
      } else {
        addNormalFieldFlow(curr, node.fact());
      }
      addNormalCallFlow(curr, node.fact());
    } else if (system.equals(PDSSystem.CALLS)) {
      if (ContextSensitive) {
        addCallRule(
            new PopRule<>(
                wrap(curr.fact()), curr.stmt(), wrap((Fact) location), getCallWeights().pop(curr)));
      }
    }
  }

  private void applyCallSummary(Stmt callSite, Fact factInCallee, Stmt spInCallee) {
    callAutomaton.addSummaryListener(
        t -> {
          GeneratedState<Fact, Stmt> genSt = ((GeneratedState<Fact, Stmt>) t.getTarget());
          Stmt sp = genSt.location();
          Fact v = genSt.node().fact();
          Stmt exitStmt = t.getLabel();
          Fact returnedFact = t.getStart().fact();
          if (spInCallee.equals(sp) && factInCallee.equals(v)) {
            if (summaries.add(
                new Summary(callSite, factInCallee, spInCallee, exitStmt, returnedFact))) {
              for (OnAddedSummaryListener<Stmt, Fact> s : Lists.newArrayList(summaryListeners)) {
                s.apply(callSite, factInCallee, spInCallee, exitStmt, returnedFact);
              }
            }
            // TODO can be removed and
            applyCallSummary(callSite, factInCallee, spInCallee, exitStmt, returnedFact);
          }
        });
  }

  Set<Summary> summaries = Sets.newHashSet();
  Set<OnAddedSummaryListener> summaryListeners = Sets.newHashSet();

  public void addApplySummaryListener(OnAddedSummaryListener l) {
    if (summaryListeners.add(l)) {
      for (Summary s : Lists.newArrayList(summaries)) {
        l.apply(s.callSite, s.factInCallee, s.spInCallee, s.exitStmt, s.returnedFact);
      }
    }
  }

  public interface OnAddedSummaryListener<Stmt, Fact> {
    void apply(Stmt callSite, Fact factInCallee, Stmt spInCallee, Stmt exitStmt, Fact returnedFact);
  }

  private class Summary {
    private final Stmt callSite;
    private final Fact factInCallee;
    private final Stmt spInCallee;
    private final Stmt exitStmt;
    private final Fact returnedFact;

    private Summary(
        Stmt callSite, Fact factInCallee, Stmt spInCallee, Stmt exitStmt, Fact returnedFact) {
      this.callSite = callSite;
      this.factInCallee = factInCallee;
      this.spInCallee = spInCallee;
      this.exitStmt = exitStmt;
      this.returnedFact = returnedFact;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Summary summary = (Summary) o;
      return Objects.equal(callSite, summary.callSite)
          && Objects.equal(factInCallee, summary.factInCallee)
          && Objects.equal(spInCallee, summary.spInCallee)
          && Objects.equal(exitStmt, summary.exitStmt)
          && Objects.equal(returnedFact, summary.returnedFact);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(callSite, factInCallee, spInCallee, exitStmt, returnedFact);
    }
  }

  public abstract void applyCallSummary(
      Stmt callSite, Fact factInCallee, Stmt spInCallee, Stmt exitStmt, Fact returnedFact);

  public void processPush(
      Node<Stmt, Fact> curr, Location location, PushNode<Stmt, Fact, ?> succ, PDSSystem system) {
    if (system.equals(PDSSystem.FIELDS)) {

      if (FieldSensitive) {
        addFieldRule(
            new PushRule<>(
                asFieldFact(curr),
                fieldWildCard(),
                asFieldFact(succ),
                (Field) location,
                fieldWildCard(),
                getFieldWeights().push(curr, succ, (Field) location)));
      } else {
        addNormalFieldFlow(curr, succ);
      }
      addNormalCallFlow(curr, succ);

    } else if (system.equals(PDSSystem.CALLS)) {
      addNormalFieldFlow(curr, succ);
      if (ContextSensitive) {
        addCallRule(
            new PushRule<>(
                wrap(curr.fact()),
                curr.stmt(),
                wrap(succ.fact()),
                succ.stmt(),
                (Stmt) location,
                getCallWeights().push(curr, succ, (Stmt) location)));
      } else {
        addNormalCallFlow(curr, succ);
      }
      applyCallSummary((Stmt) location, succ.fact(), succ.stmt());
    }
  }

  public void addCallRule(Rule<Stmt, INode<Fact>, W> rule) {
    callingPDS.addRule(rule);
  }

  public void addFieldRule(Rule<Field, INode<Node<Stmt, Fact>>, W> rule) {
    fieldPDS.addRule(rule);
  }

  public abstract WeightFunctions<Stmt, Fact, Field, W> getFieldWeights();

  public abstract WeightFunctions<Stmt, Fact, Stmt, W> getCallWeights();

  private class FieldUpdateListener
      implements WPAUpdateListener<Field, INode<Node<Stmt, Fact>>, W> {

    @Override
    public void onWeightAdded(
        Transition<Field, INode<Node<Stmt, Fact>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> aut) {
      INode<Node<Stmt, Fact>> n = t.getStart();
      if (!(n instanceof GeneratedState) && !t.getLabel().equals(fieldAutomaton.epsilon())) {
        Node<Stmt, Fact> fact = n.fact();
        Node<Stmt, Fact> node = new Node<Stmt, Fact>(fact.stmt(), fact.fact());
        setFieldContextReachable(node);
      }
    }
  }

  private void setCallingContextReachable(Node<Stmt, Fact> node) {
    if (!callingContextReachable.add(node)) return;
    if (fieldContextReachable.contains(node)) {
      processNode(node);
    }
  }

  private void setFieldContextReachable(Node<Stmt, Fact> node) {
    if (!fieldContextReachable.add(node)) {
      return;
    }
    if (callingContextReachable.contains(node)) {
      processNode(node);
    }
  }

  public void registerListener(SyncPDSUpdateListener<Stmt, Fact> listener) {
    if (!updateListeners.add(listener)) {
      return;
    }
    for (Node<Stmt, Fact> reachableNode : Lists.newArrayList(reachedStates)) {
      listener.onReachableNodeAdded(reachableNode);
    }
  }

  public void registerListener(SyncStatePDSUpdateListener<Stmt, Fact> listener) {
    if (!reachedStateUpdateListeners.put(listener.getNode(), listener)) {
      return;
    }
    if (reachedStates.contains(listener.getNode())) {
      listener.reachable();
    }
  }

  protected INode<Fact> wrap(Fact variable) {
    return new SingleNode<Fact>(variable);
  }

  protected Map<Entry<INode<Fact>, Stmt>, INode<Fact>> generatedCallState = Maps.newHashMap();

  public INode<Fact> generateCallState(final INode<Fact> d, final Stmt loc) {
    Entry<INode<Fact>, Stmt> e = new AbstractMap.SimpleEntry<>(d, loc);
    if (!generatedCallState.containsKey(e)) {
      generatedCallState.put(e, new GeneratedState<Fact, Stmt>(d, loc));
    }
    return generatedCallState.get(e);
  }

  Map<Entry<INode<Node<Stmt, Fact>>, Field>, INode<Node<Stmt, Fact>>> generatedFieldState =
      Maps.newHashMap();

  public INode<Node<Stmt, Fact>> generateFieldState(
      final INode<Node<Stmt, Fact>> d, final Field loc) {
    Entry<INode<Node<Stmt, Fact>>, Field> e = new AbstractMap.SimpleEntry<>(d, loc);
    if (!generatedFieldState.containsKey(e)) {
      generatedFieldState.put(e, new GeneratedState<Node<Stmt, Fact>, Field>(d, loc));
    }
    return generatedFieldState.get(e);
  }

  public void addGeneratedFieldState(GeneratedState<Node<Stmt, Fact>, Field> state) {
    Entry<INode<Node<Stmt, Fact>>, Field> e =
        new AbstractMap.SimpleEntry<>(state.node(), state.location());
    generatedFieldState.put(e, state);
  }

  public abstract void computeSuccessor(Node<Stmt, Fact> node);

  public abstract Field epsilonField();

  public abstract Field emptyField();

  public abstract Stmt epsilonStmt();

  public abstract Field exclusionFieldWildCard(Field exclusion);

  public abstract Field fieldWildCard();

  public Set<Node<Stmt, Fact>> getReachedStates() {
    return Sets.newHashSet(reachedStates);
  }

  public void debugOutput() {
    logger.debug(this.getClass().toString());
    logger.debug("All reachable states");
    prettyPrintSet(getReachedStates());

    HashSet<Node<Stmt, Fact>> notFieldReachable = Sets.newHashSet(callingContextReachable);
    notFieldReachable.removeAll(getReachedStates());
    HashSet<Node<Stmt, Fact>> notCallingContextReachable = Sets.newHashSet(fieldContextReachable);
    notCallingContextReachable.removeAll(getReachedStates());
    if (!notFieldReachable.isEmpty()) {
      logger.debug("Calling context reachable");
      prettyPrintSet(notFieldReachable);
    }
    if (!notCallingContextReachable.isEmpty()) {
      logger.debug("Field matching reachable");
      prettyPrintSet(notCallingContextReachable);
    }
    logger.debug(fieldPDS.toString());
    logger.debug(fieldAutomaton.toDotString());
    logger.debug(callingPDS.toString());
    logger.debug(callAutomaton.toDotString());
    logger.debug("===== end === " + this.getClass());
  }

  private void prettyPrintSet(Collection<? extends Object> set) {
    int j = 0;
    String s = "";
    for (Object reachableState : set) {
      s += reachableState;
      s += "\t";
      if (j++ > 5) {
        s += "\n";
        j = 0;
      }
    }
    logger.debug(s);
  }
}
