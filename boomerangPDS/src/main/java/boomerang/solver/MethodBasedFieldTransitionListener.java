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
import boomerang.scene.Method;
import boomerang.scene.Val;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAUpdateListener;

public abstract class MethodBasedFieldTransitionListener<W extends Weight>
    implements WPAUpdateListener<Field, INode<Node<ControlFlowGraph.Edge, Val>>, W> {
  private final Method method;

  public MethodBasedFieldTransitionListener(Method method) {
    this.method = method;
  }

  public Method getMethod() {
    return method;
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
}
