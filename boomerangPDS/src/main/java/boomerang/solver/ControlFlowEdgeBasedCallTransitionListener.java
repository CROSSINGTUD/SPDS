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
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Val;
import sync.pds.solver.nodes.INode;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAUpdateListener;

public abstract class ControlFlowEdgeBasedCallTransitionListener<W extends Weight>
    implements WPAUpdateListener<Edge, INode<Val>, W> {

  private final Edge edge;

  public ControlFlowEdgeBasedCallTransitionListener(Edge edge) {
    this.edge = edge;
  }

  public ControlFlowGraph.Edge getControlFlowEdge() {
    return edge;
  }

  @Override
  public void onWeightAdded(
      Transition<ControlFlowGraph.Edge, INode<Val>> t,
      W w,
      WeightedPAutomaton<Edge, INode<Val>, W> aut) {
    onAddedTransition(t, w);
  }

  public abstract void onAddedTransition(Transition<ControlFlowGraph.Edge, INode<Val>> t, W w);

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((edge == null) ? 0 : edge.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ControlFlowEdgeBasedCallTransitionListener other =
        (ControlFlowEdgeBasedCallTransitionListener) obj;
    if (edge == null) {
      if (other.edge != null) return false;
    } else if (!edge.equals(other.edge)) return false;
    return true;
  }
}
