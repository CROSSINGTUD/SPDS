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

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Val;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import ideal.IDEALSeedSolver.Phases;
import java.util.Collection;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.PushNode;
import wpds.impl.Weight;

public class IDEALWeightFunctions<W extends Weight> implements WeightFunctions<Edge, Val, Edge, W> {

  private static final Logger logger = LoggerFactory.getLogger(IDEALWeightFunctions.class);
  private WeightFunctions<Edge, Val, Edge, W> delegate;
  private Set<NonOneFlowListener> listeners = Sets.newHashSet();
  private Set<Edge> potentialStrongUpdates = Sets.newHashSet();
  private Set<Edge> weakUpdates = Sets.newHashSet();
  private Set<Node<Edge, Val>> nonOneFlowNodes = Sets.newHashSet();
  private Phases phase;
  private boolean strongUpdates;
  private Multimap<Node<Edge, Val>, Node<Edge, Val>> indirectAlias = HashMultimap.create();
  private Set<Node<Edge, Val>> nodesWithStrongUpdate = Sets.newHashSet();

  public IDEALWeightFunctions(WeightFunctions<Edge, Val, Edge, W> delegate, boolean strongUpdates) {
    this.delegate = delegate;
    this.strongUpdates = strongUpdates;
  }

  @Override
  public W push(Node<Edge, Val> curr, Node<Edge, Val> succ, Edge calleeSp) {
    W weight = delegate.push(curr, succ, calleeSp);
    if (isObjectFlowPhase() && !weight.equals(getOne())) {
      if (succ instanceof PushNode) {
        PushNode<Edge, Val, Edge> pushNode = (PushNode<Edge, Val, Edge>) succ;
        addOtherThanOneWeight(new Node<>(pushNode.location(), curr.fact()));
      }
    }
    return weight;
  }

  void addOtherThanOneWeight(Node<Edge, Val> curr) {
    if (nonOneFlowNodes.add(curr)) {
      for (NonOneFlowListener l : Lists.newArrayList(listeners)) {
        l.nonOneFlow(curr);
      }
    }
  }

  @Override
  public W normal(Node<Edge, Val> curr, Node<Edge, Val> succ) {
    W weight = delegate.normal(curr, succ);
    if (isObjectFlowPhase()
        && succ.stmt().getTarget().containsInvokeExpr()
        && !weight.equals(getOne())) {
      addOtherThanOneWeight(succ);
    }
    return weight;
  }

  private boolean isObjectFlowPhase() {
    return phase.equals(Phases.ObjectFlow);
  }

  private boolean isValueFlowPhase() {
    return phase.equals(Phases.ValueFlow);
  }

  @Override
  public W pop(Node<Edge, Val> curr) {
    return delegate.pop(curr);
  }

  public void registerListener(NonOneFlowListener listener) {
    if (listeners.add(listener)) {
      for (Node<Edge, Val> existing : Lists.newArrayList(nonOneFlowNodes)) {
        listener.nonOneFlow(existing);
      }
    }
  }

  @Override
  public W getOne() {
    return delegate.getOne();
  }

  @Override
  public String toString() {
    return "[IDEAL-Wrapped Weights] " + delegate.toString();
  }

  public void potentialStrongUpdate(Edge stmt) {
    potentialStrongUpdates.add(stmt);
  }

  public void weakUpdate(Edge stmt) {
    weakUpdates.add(stmt);
  }

  public void setPhase(Phases phase) {
    this.phase = phase;
  }

  public void addIndirectFlow(Node<Edge, Val> source, Node<Edge, Val> target) {
    if (source.equals(target)) return;
    logger.trace("Alias flow detected " + source + " " + target);
    indirectAlias.put(source, target);
  }

  public Collection<Node<Edge, Val>> getAliasesFor(Node<Edge, Val> node) {
    return indirectAlias.get(node);
  }

  public boolean isStrongUpdateStatement(Edge stmt) {
    return potentialStrongUpdates.contains(stmt) && !weakUpdates.contains(stmt) && strongUpdates;
  }

  public boolean isKillFlow(Node<Edge, Val> node) {
    return !nodesWithStrongUpdate.contains(node);
  }

  public void addNonKillFlow(Node<Edge, Val> curr) {
    nodesWithStrongUpdate.add(curr);
  }
}
