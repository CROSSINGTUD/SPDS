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

public class NodeWithLocation<Stmt, Fact, Location> implements INode<Node<Stmt, Fact>> {

  private Location loc;
  private Node<Stmt, Fact> fact;

  public NodeWithLocation(Stmt stmt, Fact variable, Location loc) {
    this.fact = new Node<>(stmt, variable);
    this.loc = loc;
  }

  @Override
  public Node<Stmt, Fact> fact() {
    return fact;
  }

  public Location location() {
    return loc;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fact == null) ? 0 : fact.hashCode());
    result = prime * result + ((loc == null) ? 0 : loc.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    NodeWithLocation other = (NodeWithLocation) obj;
    if (fact == null) {
      if (other.fact != null) return false;
    } else if (!fact.equals(other.fact)) return false;
    if (loc == null) {
      if (other.loc != null) return false;
    } else if (!loc.equals(other.loc)) return false;
    return true;
  }

  @Override
  public String toString() {
    return fact + " loc: " + loc;
  }
}
