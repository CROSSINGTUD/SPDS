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
import sync.pds.solver.nodes.Node;

public interface NonOneFlowListener {

  void nonOneFlow(Node<Edge, Val> curr);
}
