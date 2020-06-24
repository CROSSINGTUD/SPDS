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
package wpds.impl;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pathexpression.Edge;
import pathexpression.IRegEx;
import pathexpression.LabeledGraph;
import pathexpression.PathExpressionComputer;
import pathexpression.RegEx;
import wpds.interfaces.ForwardDFSEpsilonVisitor;
import wpds.interfaces.ForwardDFSVisitor;
import wpds.interfaces.Location;
import wpds.interfaces.ReachabilityListener;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public abstract class WeightedPAutomaton<N extends Location, D extends State, W extends Weight>
    implements LabeledGraph<D, N> {
  private static final Logger LOGGER = LoggerFactory.getLogger(WeightedPAutomaton.class);
  private Map<Transition<N, D>, W> transitionToWeights = new HashMap<>();
  // Set Q is implicit
  // Weighted Pushdown Systems and their Application to Interprocedural
  // Dataflow Analysis
  protected Set<Transition<N, D>> transitions = Sets.newHashSet();
  // set F in paper [Reps2003]
  protected Set<D> finalState = Sets.newHashSet();
  protected Multimap<D, D> initialStatesToSource = HashMultimap.create();
  // set P in paper [Reps2003]
  protected Set<D> states = Sets.newHashSet();
  private final Multimap<D, Transition<N, D>> transitionsOutOf = HashMultimap.create();
  private final Multimap<D, Transition<N, D>> transitionsInto = HashMultimap.create();
  private Set<WPAUpdateListener<N, D, W>> listeners = Sets.newHashSet();
  private Multimap<D, WPAStateListener<N, D, W>> stateListeners = HashMultimap.create();
  private Map<D, ForwardDFSVisitor<N, D, W>> stateToDFS = Maps.newHashMap();
  private Map<D, ForwardDFSVisitor<N, D, W>> stateToEpsilonDFS = Maps.newHashMap();
  private Set<WeightedPAutomaton<N, D, W>> nestedAutomatons = Sets.newHashSet();
  private Set<NestedAutomatonListener<N, D, W>> nestedAutomataListeners = Sets.newHashSet();
  private Map<D, ReachabilityListener<N, D>> stateToEpsilonReachabilityListener = Maps.newHashMap();
  private Map<D, ReachabilityListener<N, D>> stateToReachabilityListener = Maps.newHashMap();
  private Set<ReturnSiteWithWeights> connectedPushes = Sets.newHashSet();
  private Set<ConnectPushListener<N, D, W>> conntectedPushListeners = Sets.newHashSet();
  private Set<UnbalancedPopListener<N, D, W>> unbalancedPopListeners = Sets.newHashSet();
  private Map<UnbalancedPopEntry, W> unbalancedPops = Maps.newHashMap();
  private Map<Transition<N, D>, W> transitionsToFinalWeights = Maps.newHashMap();
  private ForwardDFSVisitor<N, D, W> dfsVisitor;
  private ForwardDFSVisitor<N, D, W> dfsEpsVisitor;
  public int failedAdditions;
  public int failedDirectAdditions;
  private WeightedPAutomaton<N, D, W> initialAutomaton;
  private PathExpressionComputer<D, N> pathExpressionComputer;
  private int lastStates = 0;
  private Stopwatch watch = Stopwatch.createUnstarted();
  private Map<D, Integer> stateToDistanceToInitial = Maps.newHashMap();
  private Map<D, Integer> stateToUnbalancedDistance = Maps.newHashMap();
  private final Map<D, Transition<N, D>> stateCreatingTransition = Maps.newHashMap();

  public abstract D createState(D d, N loc);

  public abstract boolean isGeneratedState(D d);

  public Collection<Transition<N, D>> getTransitions() {
    return Lists.newArrayList(transitions);
  }

  public boolean addTransition(Transition<N, D> trans) {
    boolean addWeightForTransition = addWeightForTransition(trans, getOne());
    if (!addWeightForTransition) {
      failedDirectAdditions++;
    }
    return addWeightForTransition;
  }

  public Set<D> getFinalState() {
    return finalState;
  }

  public String toString() {
    String s = "PAutomaton\n";
    s += "\tInitialStates:" + initialStatesToSource.keySet() + "\n";
    s += "\tFinalStates:" + finalState + "\n";
    s += "\tWeightToTransitions:\n\t\t";
    s += Joiner.on("\n\t\t").join(transitionToWeights.entrySet());

    for (WeightedPAutomaton<N, D, W> nested : nestedAutomatons) {
      s += "\n";
      s += nested.toString();
    }
    return s;
  }

  private String wrapIfInitialOrFinalState(D s) {
    return initialStatesToSource.containsKey(s) ? "ENTRY: " + wrapFinalState(s) : wrapFinalState(s);
  }

  private String wrapFinalState(D s) {
    return finalState.contains(s) ? "TO: " + s + "" : s.toString();
  }

  private static final boolean SUMMARIZE = false;

  public String toDotString() {
    return toDotString(Sets.<WeightedPAutomaton<N, D, W>>newHashSet());
  }

  private String toDotString(Set<WeightedPAutomaton<N, D, W>> visited) {
    if (!visited.add(this)) {
      return "NESTED loop: " + getInitialStates();
    }
    String s = "digraph {\n";
    TreeSet<String> trans = new TreeSet<String>();
    List<String> summaryIdentifier = Lists.newArrayList();
    Set<Transition<N, D>> removableTrans = Sets.newHashSet();
    if (SUMMARIZE) {
      Table<N, D, Set<Transition<N, D>>> mergableStates = HashBasedTable.create();
      for (D source : states) {
        if (transitionsInto.get(source).isEmpty() && transitionsOutOf.get(source).size() == 1) {
          for (Transition<N, D> t : transitionsOutOf.get(source)) {
            Set<Transition<N, D>> set = mergableStates.get(t.getLabel(), t.getTarget());
            if (set == null) {
              set = Sets.newHashSet();
            }
            set.add(t);
            removableTrans.add(t);
            mergableStates.put(t.getLabel(), t.getTarget(), set);
          }
        }
      }
      for (N label : mergableStates.rowKeySet()) {
        for (D target : mergableStates.columnKeySet()) {
          Collection<Transition<N, D>> trs = mergableStates.get(label, target);
          if (trs == null) continue;
          List<String> labels = Lists.newLinkedList();
          for (Transition<N, D> t : trs) {
            labels.add(escapeQuotes(wrapIfInitialOrFinalState(t.getStart())));
          }
          if (!labels.isEmpty()) {
            summaryIdentifier.add(Joiner.on("\\n").join(labels));
            String v = "\t\"" + "SUMNODE_" + summaryIdentifier.size() + "\"";
            v += " -> \"" + escapeQuotes(wrapIfInitialOrFinalState(target)) + "\"";
            v += "[label=\"" + escapeQuotes(label.toString()) + "\"];\n";
            trans.add(v);
          }
        }
      }
    }
    for (D source : states) {
      Collection<Transition<N, D>> collection = transitionsOutOf.get(source);

      for (D target : states) {
        List<String> labels = Lists.newLinkedList();
        for (Transition<N, D> t : collection) {
          if (removableTrans.contains(t)) continue;
          if (t.getTarget().equals(target)) {
            labels.add(escapeQuotes(t.getLabel().toString()) + " W: " + transitionToWeights.get(t));
          }
        }
        if (!labels.isEmpty()) {
          String v = "\t\"" + escapeQuotes(wrapIfInitialOrFinalState(source)) + "\"";
          v += " -> \"" + escapeQuotes(wrapIfInitialOrFinalState(target)) + "\"";
          v += "[label=\"" + Joiner.on("\\n").join(labels) + "\"];\n";
          trans.add(v);
        }
      }
    }
    s += Joiner.on("").join(trans);
    s += "}\n";
    if (SUMMARIZE) {
      int i = 1;
      for (String node : summaryIdentifier) {
        s += "SUMNODE_" + i + ":\n";
        s += node;
        s += "\n";
      }
    }

    s += "Transitions: " + transitions.size() + " Nested: " + nestedAutomatons.size() + "\n";
    for (WeightedPAutomaton<N, D, W> nested : nestedAutomatons) {
      s += "NESTED -> \n";
      s += nested.toDotString(visited);
    }
    s += "End nesting\n";
    return s;
  }

  public Set<D> getInitialStates() {
    return initialStatesToSource.keySet();
  }

  private String escapeQuotes(String string) {
    return string.replace("\"", "");
  }

  public String toLabelGroupedDotString() {
    HashBasedTable<D, N, Collection<D>> groupedByTargetAndLabel = HashBasedTable.create();
    for (Transition<N, D> t : transitions) {
      Collection<D> collection = groupedByTargetAndLabel.get(t.getTarget(), t.getLabel());
      if (collection == null) collection = Sets.newHashSet();
      collection.add(t.getStart());
      groupedByTargetAndLabel.put(t.getTarget(), t.getLabel(), collection);
    }
    String s = "digraph {\n";
    for (D target : groupedByTargetAndLabel.rowKeySet()) {
      for (N label : groupedByTargetAndLabel.columnKeySet()) {
        Collection<D> source = groupedByTargetAndLabel.get(target, label);
        if (source == null) continue;
        s += "\t\"" + Joiner.on("\\n").join(source) + "\"";
        s += " -> \"" + wrapIfInitialOrFinalState(target) + "\"";
        s += "[label=\"" + label + "\"];\n";
      }
    }
    s += "}\n";
    s += "Transitions: " + transitions.size() + "\n";
    for (WeightedPAutomaton<N, D, W> nested : nestedAutomatons) {
      s += "NESTED -> \n";
      s += nested.toDotString();
    }
    return s;
  }

  public abstract N epsilon();

  public IRegEx<N> extractLanguage(D from) {
    PathExpressionComputer<D, N> expr = new PathExpressionComputer<>(this);
    IRegEx<N> res = null;
    for (D finalState : getFinalState()) {
      IRegEx<N> regEx = expr.getExpressionBetween(from, finalState);
      if (res == null) {
        res = regEx;
      } else {
        res = RegEx.<N>union(res, regEx);
      }
    }
    if (res == null) return new RegEx.EmptySet<N>();
    return res;
  }

  public IRegEx<N> extractLanguage(D from, D to) {
    PathExpressionComputer<D, N> expr = new PathExpressionComputer<>(this);
    IRegEx<N> res = expr.getExpressionBetween(from, to);
    if (res == null) return new RegEx.EmptySet<N>();
    return res;
  }

  public Set<D> getStates() {
    return states;
  }

  public Set<Edge<D, N>> getEdges() {
    Set<Edge<D, N>> trans = Sets.newHashSet();
    for (Edge<D, N> tran : transitions) {
      if (!tran.getLabel().equals(epsilon())) {
        trans.add(new Transition<N, D>(tran.getTarget(), tran.getLabel(), tran.getStart()));
      }
    }
    return trans;
  };

  public Set<D> getNodes() {
    return getStates();
  };

  public boolean addWeightForTransition(Transition<N, D> trans, W weight) {
    if (weight == null) throw new IllegalArgumentException("Weight must not be null!");
    if (trans.getStart().equals(trans.getTarget()) && trans.getLabel().equals(epsilon())) {
      failedAdditions++;
      return false;
    }
    int distanceToInitial = computeDistance(trans);
    if (hasMaxDepth() && distanceToInitial > getMaxDepth()) {
      return false;
    }
    if (!watch.isRunning()) {
      watch.start();
    }
    transitionsOutOf.get(trans.getStart()).add(trans);
    transitionsInto.get(trans.getTarget()).add(trans);
    if (states.add(trans.getTarget())) {
      stateCreatingTransition.put(trans.getTarget(), trans);
    }
    states.add(trans.getStart());
    boolean added = transitions.add(trans);
    W oldWeight = transitionToWeights.get(trans);
    W newWeight = (W) (oldWeight == null ? weight : oldWeight.combineWith(weight));

    if (!newWeight.equals(oldWeight)) {
      transitionToWeights.put(trans, newWeight);

      for (WPAUpdateListener<N, D, W> l : Lists.newArrayList(listeners)) {
        l.onWeightAdded(trans, newWeight, this);
      }
      for (WPAStateListener<N, D, W> l : Lists.newArrayList(stateListeners.get(trans.getStart()))) {
        l.onOutTransitionAdded(trans, newWeight, this);
      }
      for (WPAStateListener<N, D, W> l :
          Lists.newArrayList(stateListeners.get(trans.getTarget()))) {
        l.onInTransitionAdded(trans, newWeight, this);
      }
      added = true;
    }
    if (watch.isRunning()) watch.stop();
    if (!added) failedAdditions++;
    return added;
  }

  protected int computeDistance(Transition<N, D> trans) {
    Integer distance;
    if (isUnbalancedState(trans.getTarget())) {
      distance = 0;
    } else {
      distance = stateToDistanceToInitial.get(trans.getTarget());
      if (distance == null) {
        return -1;
      }
    }
    Integer integer = ++distance;
    Integer currDistance = stateToDistanceToInitial.get(trans.getStart());
    if (currDistance == null || integer < currDistance) {
      stateToDistanceToInitial.put(trans.getStart(), integer);
      return integer;
    }
    return currDistance;
  }

  public W getWeightFor(Transition<N, D> trans) {
    return transitionToWeights.get(trans);
  }

  public void registerListener(WPAUpdateListener<N, D, W> listener) {
    if (!listeners.add(listener)) return;
    for (Entry<Transition<N, D>, W> transAndWeight :
        Lists.newArrayList(transitionToWeights.entrySet())) {
      listener.onWeightAdded(transAndWeight.getKey(), transAndWeight.getValue(), this);
    }
    for (WeightedPAutomaton<N, D, W> nested : Lists.newArrayList(nestedAutomatons)) {
      nested.registerListener(listener);
    }
  }

  private static int count = 0;

  private void increaseListenerCount(WPAStateListener<N, D, W> l) {
    count++;
    if (count % 100000 == 0) {
      onManyStateListenerRegister();
    }
  }

  public void onManyStateListenerRegister() {}

  public void registerListener(WPAStateListener<N, D, W> l) {
    if (!stateListeners.put(l.getState(), l)) {
      return;
    }
    increaseListenerCount(l);
    for (Transition<N, D> t : Lists.newArrayList(transitionsOutOf.get(l.getState()))) {
      l.onOutTransitionAdded(t, transitionToWeights.get(t), this);
    }
    for (Transition<N, D> t : Lists.newArrayList(transitionsInto.get(l.getState()))) {
      l.onInTransitionAdded(t, transitionToWeights.get(t), this);
    }

    for (WeightedPAutomaton<N, D, W> nested : Lists.newArrayList(nestedAutomatons)) {
      nested.registerListener(l);
    }
  }

  public void addFinalState(D state) {
    this.finalState.add(state);
  }

  public void registerDFSListener(D state, ReachabilityListener<N, D> l) {
    stateToReachabilityListener.put(state, l);
    if (dfsVisitor == null) {
      dfsVisitor = new ForwardDFSVisitor<N, D, W>(this);
      this.registerListener(dfsVisitor);
    }
    dfsVisitor.registerListener(state, l);
  }

  protected Map<D, ForwardDFSVisitor<N, D, W>> getStateToDFS() {
    return stateToDFS;
  }

  public void registerDFSEpsilonListener(D state, ReachabilityListener<N, D> l) {
    stateToEpsilonReachabilityListener.put(state, l);
    if (dfsEpsVisitor == null) {
      dfsEpsVisitor = new ForwardDFSEpsilonVisitor<N, D, W>(this);
      this.registerListener(dfsEpsVisitor);
    }
    for (WeightedPAutomaton<N, D, W> nested : Lists.newLinkedList(nestedAutomatons)) {
      nested.registerDFSEpsilonListener(state, l);
    }
    dfsEpsVisitor.registerListener(state, l);
  }

  protected Map<D, ForwardDFSVisitor<N, D, W>> getStateToEpsilonDFS() {
    return stateToEpsilonDFS;
  }

  public abstract W getOne();

  public WeightedPAutomaton<N, D, W> createNestedAutomaton(D initialState) {
    WeightedPAutomaton<N, D, W> nested =
        new WeightedPAutomaton<N, D, W>() {

          @Override
          public D createState(D d, N loc) {
            return WeightedPAutomaton.this.createState(d, loc);
          }

          @Override
          public N epsilon() {
            return WeightedPAutomaton.this.epsilon();
          }

          @Override
          public W getOne() {
            return WeightedPAutomaton.this.getOne();
          }

          @Override
          public boolean isGeneratedState(D d) {
            return WeightedPAutomaton.this.isGeneratedState(d);
          }

          @Override
          protected Map<D, ForwardDFSVisitor<N, D, W>> getStateToDFS() {
            return WeightedPAutomaton.this.stateToDFS;
          }

          @Override
          protected Map<D, ForwardDFSVisitor<N, D, W>> getStateToEpsilonDFS() {
            return WeightedPAutomaton.this.stateToEpsilonDFS;
          }

          @Override
          public boolean nested() {
            return true;
          }

          @Override
          public String toString() {
            return "NESTED: \n" + super.toString();
          }
        };
    addNestedAutomaton(nested);
    return nested;
  }

  public void registerUnbalancedPopListener(UnbalancedPopListener<N, D, W> l) {
    if (unbalancedPopListeners.add(l)) {
      for (Entry<UnbalancedPopEntry, W> e : Lists.newArrayList(unbalancedPops.entrySet())) {
        UnbalancedPopEntry t = e.getKey();
        l.unbalancedPop(t.targetState, t.trans, e.getValue());
      }
    }
  }

  public void unbalancedPop(D targetState, Transition<N, D> trans, W weight) {
    UnbalancedPopEntry t = new UnbalancedPopEntry(targetState, trans);
    W oldVal = unbalancedPops.get(t);
    W newVal = (oldVal == null ? weight : (W) oldVal.combineWith(weight));
    if (!newVal.equals(oldVal)) {
      unbalancedPops.put(t, newVal);
      for (UnbalancedPopListener<N, D, W> l : Lists.newArrayList(unbalancedPopListeners)) {
        l.unbalancedPop(targetState, trans, newVal);
      }
    }
  }

  private Set<Transition<N, D>> summaryEdges = Sets.newHashSet();
  private Set<SummaryListener<N, D>> summaryEdgeListener = Sets.newHashSet();

  public void registerSummaryEdge(Transition<N, D> t) {
    if (summaryEdges.add(t)) {
      for (SummaryListener<N, D> l : Lists.newArrayList(summaryEdgeListener)) {
        l.addedSummary(t);
      }
    }
  }

  public void addSummaryListener(SummaryListener<N, D> l) {
    if (summaryEdgeListener.add(l)) {
      for (Transition<N, D> edge : Lists.newArrayList(summaryEdges)) {
        l.addedSummary(edge);
      }
      for (WeightedPAutomaton<N, D, W> nested : Lists.newArrayList(nestedAutomatons)) {
        nested.addSummaryListener(l);
      }
    }
  }

  public interface SummaryListener<N extends Location, D extends State> {
    void addedSummary(Transition<N, D> t);
  }

  private class UnbalancedPopEntry {

    private final Transition<N, D> trans;
    private final D targetState;

    public UnbalancedPopEntry(D targetState, Transition<N, D> trans) {
      this.targetState = targetState;
      this.trans = trans;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((targetState == null) ? 0 : targetState.hashCode());
      result = prime * result + ((trans == null) ? 0 : trans.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      UnbalancedPopEntry other = (UnbalancedPopEntry) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (targetState == null) {
        if (other.targetState != null) return false;
      } else if (!targetState.equals(other.targetState)) return false;
      if (trans == null) {
        if (other.trans != null) return false;
      } else if (!trans.equals(other.trans)) return false;
      return true;
    }

    private WeightedPAutomaton getOuterType() {
      return WeightedPAutomaton.this;
    }
  }

  private class ReturnSiteWithWeights {

    private final N returnSite;
    private final W returnedWeight;
    private final D returnedFact;

    public ReturnSiteWithWeights(N returnSite, D returnedFact, W returnedWeight) {
      this.returnSite = returnSite;
      this.returnedFact = returnedFact;
      this.returnedWeight = returnedWeight;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((returnSite == null) ? 0 : returnSite.hashCode());
      result = prime * result + ((returnedFact == null) ? 0 : returnedFact.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ReturnSiteWithWeights other = (ReturnSiteWithWeights) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (returnSite == null) {
        if (other.returnSite != null) return false;
      } else if (!returnSite.equals(other.returnSite)) return false;
      if (returnedFact == null) {
        if (other.returnedFact != null) return false;
      } else if (!returnedFact.equals(other.returnedFact)) return false;
      if (returnedWeight == null) {
        if (other.returnedWeight != null) return false;
      } else if (!returnedWeight.equals(other.returnedWeight)) return false;
      return true;
    }

    private WeightedPAutomaton getOuterType() {
      return WeightedPAutomaton.this;
    }
  }

  public Map<Transition<N, D>, W> getTransitionsToFinalWeights() {
    LOGGER.trace("Start computing final weights");
    final Stopwatch w = Stopwatch.createStarted();
    for (D s : initialStatesToSource.keySet()) {
      registerListener(new ValueComputationListener(s, getOne()));
    }
    LOGGER.trace("Finished computing final weights in {}", w);
    return transitionsToFinalWeights;
  }

  private class ValueComputationListener extends WPAStateListener<N, D, W> {

    private W weight;

    public ValueComputationListener(D state, W weight) {
      super(state);
      this.weight = weight;
    }

    @Override
    public void onOutTransitionAdded(Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> aut) {}

    @Override
    public void onInTransitionAdded(Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> aut) {
      W newWeight = (W) weight.extendWith(w);
      W weightAtTarget = transitionsToFinalWeights.get(t);
      W newVal = (weightAtTarget == null ? newWeight : (W) weightAtTarget.combineWith(newWeight));
      transitionsToFinalWeights.put(t, newVal);
      if (isGeneratedState(t.getStart())) {
        registerListener(new ValueComputationListener(t.getStart(), newVal));
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((weight == null) ? 0 : weight.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      ValueComputationListener other = (ValueComputationListener) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (weight == null) {
        if (other.weight != null) return false;
      } else if (!weight.equals(other.weight)) return false;
      return true;
    }

    private WeightedPAutomaton getOuterType() {
      return WeightedPAutomaton.this;
    }
  }

  public boolean nested() {
    return false;
  }

  public void addNestedAutomaton(WeightedPAutomaton<N, D, W> nested) {
    if (!nestedAutomatons.add(nested)) return;
    for (WPAStateListener<N, D, W> e : Lists.newArrayList(stateListeners.values())) {
      nested.registerListener(e);
    }
    for (WPAUpdateListener<N, D, W> e : Lists.newArrayList(listeners)) {
      nested.registerListener(e);
    }

    for (SummaryListener<N, D> e : Lists.newArrayList(summaryEdgeListener)) {
      nested.addSummaryListener(e);
    }

    for (UnbalancedPopListener<N, D, W> e : Lists.newArrayList(unbalancedPopListeners)) {
      nested.registerUnbalancedPopListener(e);
    }

    for (Entry<D, ReachabilityListener<N, D>> e :
        Lists.newArrayList(stateToEpsilonReachabilityListener.entrySet())) {
      nested.registerDFSEpsilonListener(e.getKey(), e.getValue());
    }
    for (Entry<D, ReachabilityListener<N, D>> e :
        Lists.newArrayList(stateToReachabilityListener.entrySet())) {
      nested.registerDFSListener(e.getKey(), e.getValue());
    }
    for (Entry<D, ReachabilityListener<N, D>> e :
        Lists.newArrayList(stateToReachabilityListener.entrySet())) {
      nested.registerDFSListener(e.getKey(), e.getValue());
    }

    for (NestedAutomatonListener<N, D, W> e : Lists.newArrayList(nestedAutomataListeners)) {
      e.nestedAutomaton(this, nested);
      nested.registerNestedAutomatonListener(e);
    }
  }

  public void registerNestedAutomatonListener(NestedAutomatonListener<N, D, W> l) {
    if (!nestedAutomataListeners.add(l)) {
      return;
    }
    for (WeightedPAutomaton<N, D, W> nested : Lists.newArrayList(nestedAutomatons)) {
      l.nestedAutomaton(this, nested);
    }
  }

  public void setInitialAutomaton(WeightedPAutomaton<N, D, W> aut) {
    initialAutomaton = aut;
  }

  public boolean isInitialAutomaton(WeightedPAutomaton<N, D, W> aut) {
    return initialAutomaton.equals(aut);
  }

  public IRegEx<N> toRegEx(D start, D end) {
    if (lastStates < states.size()) {
      pathExpressionComputer = new PathExpressionComputer<D, N>(this);
      lastStates = states.size();
    }

    return RegEx.reverse(pathExpressionComputer.getExpressionBetween(end, start));
  }

  public boolean containsLoop() {
    // Performs a backward DFS
    HashSet<D> visited = Sets.newHashSet();
    LinkedList<D> worklist = Lists.newLinkedList();
    worklist.addAll(initialStatesToSource.keySet());
    while (!worklist.isEmpty()) {
      D pop = worklist.pop();
      visited.add(pop);
      Collection<Transition<N, D>> inTrans = transitionsInto.get(pop);
      for (Transition<N, D> t : inTrans) {
        if (t.getLabel().equals(this.epsilon())) continue;
        if (!isGeneratedState(t.getStart())) continue;
        if (visited.contains(t.getStart())) {
          return true;
        }
        worklist.add(t.getStart());
      }
    }
    return false;
  }

  public Set<N> getLongestPath() {
    // Performs a backward DFS
    LinkedList<D> worklist = Lists.newLinkedList();
    worklist.addAll(initialStatesToSource.keySet());
    Map<D, Set<N>> pathReachingD = Maps.newHashMap();
    while (!worklist.isEmpty()) {
      D pop = worklist.pop();
      Set<N> atCurr = getOrCreate(pathReachingD, pop);
      Collection<Transition<N, D>> inTrans = transitionsInto.get(pop);
      for (Transition<N, D> t : inTrans) {
        if (t.getLabel().equals(this.epsilon())) continue;
        D next = t.getStart();
        if (!isGeneratedState(next)) continue;
        if (next.equals(pop)) continue;
        Set<N> atNext = getOrCreate(pathReachingD, next);
        Set<N> newAtCurr = Sets.newHashSet(atCurr);
        if (newAtCurr.add(t.getLabel())) {
          boolean addAll = atNext.addAll(newAtCurr);
          if (addAll) {
            worklist.add(next);
          }
        }
      }
    }
    Set<N> longest = Sets.newHashSet();
    for (Set<N> l : pathReachingD.values()) {
      if (longest.size() < l.size()) {
        longest = l;
      }
    }
    return longest;
  }

  private Set<N> getOrCreate(Map<D, Set<N>> pathReachingD, D pop) {
    Set<N> collection = pathReachingD.get(pop);
    if (collection == null) {
      collection = Sets.newHashSet();
      pathReachingD.put(pop, collection);
    }
    return collection;
  }

  public boolean isUnbalancedState(D target) {
    return initialStatesToSource.containsKey(target);
  }

  public boolean addUnbalancedState(D state, D parent) {
    Integer distance = 0;
    Collection<D> parents = Sets.newHashSet();
    if (!initialStatesToSource.containsKey(parent)) {
      distance = stateToUnbalancedDistance.get(parent);
      parents.add(parent);
    } else {
      parents.addAll(initialStatesToSource.get(parent));
    }
    Integer newDistance = ++distance;
    stateToUnbalancedDistance.put(state, newDistance);
    if (getMaxUnbalancedDepth() > 0 && newDistance > getMaxUnbalancedDepth()) {
      return false;
    }
    initialStatesToSource.putAll(state, parents);
    return true;
  }

  public boolean addInitialState(D state) {
    return initialStatesToSource.put(state, state);
  }

  public void unregisterAllListeners() {
    this.conntectedPushListeners.clear();
    this.nestedAutomataListeners.clear();
    this.stateListeners.clear();
    this.listeners.clear();
    this.stateToEpsilonReachabilityListener.clear();
    this.stateToReachabilityListener.clear();
    this.summaryEdgeListener.clear();
    this.unbalancedPopListeners.clear();
  }

  public Stopwatch getWatch() {
    return watch;
  }

  public boolean hasMaxDepth() {
    return getMaxDepth() > 0;
  }

  public int getMaxDepth() {
    return -1;
  }

  public int getMaxUnbalancedDepth() {
    return -1;
  }

  public Collection<D> getUnbalancedStartOf(D target) {
    return initialStatesToSource.get(target);
  }
}
