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
package boomerang;

import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Type;
import boomerang.scene.Val;
import sync.pds.solver.nodes.Node;

public abstract class Query {

  private final ControlFlowGraph.Edge cfgEdge;
  private final Val variable;

  public Query(ControlFlowGraph.Edge cfgEdge, Val variable) {
    this.cfgEdge = cfgEdge;
    this.variable = variable;
  }

  public Node<ControlFlowGraph.Edge, Val> asNode() {
    return new Node<>(cfgEdge, variable);
  }

  @Override
  public String toString() {
    return new Node<>(cfgEdge, variable).toString();
  }

  public Edge cfgEdge() {
    return cfgEdge;
  }

  public Val var() {
    return variable;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cfgEdge == null) ? 0 : cfgEdge.hashCode());
    result = prime * result + ((variable == null) ? 0 : variable.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (obj.getClass() != this.getClass()) return false;
    Query other = (Query) obj;
    if (cfgEdge == null) {
      if (other.cfgEdge != null) return false;
    } else if (!cfgEdge.equals(other.cfgEdge)) return false;
    if (variable == null) {
      if (other.variable != null) return false;
    } else if (!variable.equals(other.variable)) return false;
    return true;
  }

  public Type getType() {
    if (variable instanceof AllocVal) {
      AllocVal allocVal = (AllocVal) variable;
      return allocVal.getAllocVal().getType();
    }
    return variable.getType();
  }
}
