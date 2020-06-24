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
package sync.pds.solver.nodes;

public class ExclusionNode<Stmt, Fact, Location> extends Node<Stmt, Fact> {

  private Location exclusion;

  public ExclusionNode(Stmt stmt, Fact variable, Location exclusion) {
    super(stmt, variable);
    this.exclusion = exclusion;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((exclusion == null) ? 0 : exclusion.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    ExclusionNode other = (ExclusionNode) obj;
    if (exclusion == null) {
      if (other.exclusion != null) return false;
    } else if (!exclusion.equals(other.exclusion)) return false;
    return true;
  }

  public Location exclusion() {
    return exclusion;
  }
}
