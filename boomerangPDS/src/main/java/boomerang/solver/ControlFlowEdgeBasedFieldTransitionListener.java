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

import boomerang.scene.ControlFlowGraph;
import boomerang.scene.Field;
import boomerang.scene.Val;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAUpdateListener;

public abstract class ControlFlowEdgeBasedFieldTransitionListener<W extends Weight>
    implements WPAUpdateListener<Field, INode<Node<ControlFlowGraph.Edge, Val>>, W> {

  private final ControlFlowGraph.Edge cfgEdge;

  public ControlFlowEdgeBasedFieldTransitionListener(ControlFlowGraph.Edge cfgEdge) {
    this.cfgEdge = cfgEdge;
  }

  public ControlFlowGraph.Edge getCfgEdge() {
    return cfgEdge;
  }

  @Override
  public void onWeightAdded(
      Transition<Field, INode<Node<ControlFlowGraph.Edge, Val>>> t,
      W w,
      WeightedPAutomaton<Field, INode<Node<ControlFlowGraph.Edge, Val>>, W> aut) {
    onAddedTransition(t);
  }

  public abstract void onAddedTransition(
      Transition<Field, INode<Node<ControlFlowGraph.Edge, Val>>> t);

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cfgEdge == null) ? 0 : cfgEdge.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ControlFlowEdgeBasedFieldTransitionListener other =
        (ControlFlowEdgeBasedFieldTransitionListener) obj;
    if (cfgEdge == null) {
      if (other.cfgEdge != null) return false;
    } else if (!cfgEdge.equals(other.cfgEdge)) return false;
    return true;
  }
}
