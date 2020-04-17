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

import boomerang.scene.Statement;
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

public class IDEALWeightFunctions<W extends Weight>
    implements WeightFunctions<Statement, Val, Statement, W> {

  private static final Logger logger = LoggerFactory.getLogger(IDEALWeightFunctions.class);
  private WeightFunctions<Statement, Val, Statement, W> delegate;
  private Set<NonOneFlowListener> listeners = Sets.newHashSet();
  private Set<Statement> potentialStrongUpdates = Sets.newHashSet();
  private Set<Statement> weakUpdates = Sets.newHashSet();
  private Set<Node<Statement, Val>> nonOneFlowNodes = Sets.newHashSet();
  private Phases phase;
  private boolean strongUpdates;
  private Multimap<Node<Statement, Val>, Node<Statement, Val>> indirectAlias =
      HashMultimap.create();
  private Set<Node<Statement, Val>> nodesWithStrongUpdate = Sets.newHashSet();

  public IDEALWeightFunctions(
      WeightFunctions<Statement, Val, Statement, W> delegate, boolean strongUpdates) {
    this.delegate = delegate;
    this.strongUpdates = strongUpdates;
  }

  @Override
  public W push(Node<Statement, Val> curr, Node<Statement, Val> succ, Statement calleeSp) {
    W weight = delegate.push(curr, succ, calleeSp);
    if (isObjectFlowPhase() && !weight.equals(getOne())) {
      if (succ instanceof PushNode) {
        PushNode<Statement, Val, Statement> pushNode = (PushNode<Statement, Val, Statement>) succ;
        addOtherThanOneWeight(new Node<>(pushNode.location(), curr.fact()));
      }
    }
    return weight;
  }

  void addOtherThanOneWeight(Node<Statement, Val> curr) {
    if (nonOneFlowNodes.add(curr)) {
      for (NonOneFlowListener l : Lists.newArrayList(listeners)) {
        l.nonOneFlow(curr);
      }
    }
  }

  @Override
  public W normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
    W weight = delegate.normal(curr, succ);
    if (isObjectFlowPhase() && succ.stmt().containsInvokeExpr() && !weight.equals(getOne())) {
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
  public W pop(Node<Statement, Val> curr) {
    return delegate.pop(curr);
  }

  public void registerListener(NonOneFlowListener listener) {
    if (listeners.add(listener)) {
      for (Node<Statement, Val> existing : Lists.newArrayList(nonOneFlowNodes)) {
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

  public void potentialStrongUpdate(Statement stmt) {
    potentialStrongUpdates.add(stmt);
  }

  public void weakUpdate(Statement stmt) {
    weakUpdates.add(stmt);
  }

  public void setPhase(Phases phase) {
    this.phase = phase;
  }

  public void addIndirectFlow(Node<Statement, Val> source, Node<Statement, Val> target) {
    if (source.equals(target)) return;
    logger.trace("Alias flow detected " + source + " " + target);
    indirectAlias.put(source, target);
  }

  public Collection<Node<Statement, Val>> getAliasesFor(Node<Statement, Val> node) {
    return indirectAlias.get(node);
  }

  public boolean isStrongUpdateStatement(Statement stmt) {
    return potentialStrongUpdates.contains(stmt) && !weakUpdates.contains(stmt) && strongUpdates;
  }

  public boolean isKillFlow(Node<Statement, Val> node) {
    return !nodesWithStrongUpdate.contains(node);
  }

  public void addNonKillFlow(Node<Statement, Val> curr) {
    nodesWithStrongUpdate.add(curr);
  }
}
