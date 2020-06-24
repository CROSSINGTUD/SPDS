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

public class GeneratedState<L, N> implements INode<L> {

  private INode<L> node;
  private N loc;

  public GeneratedState(INode<L> node, N loc) {
    this.node = node;
    this.loc = loc;
  }

  @Override
  public L fact() {
    return node.fact();
    // throw new RuntimeException("System internal state");
  }

  public INode<L> node() {
    return node;
  }

  public N location() {
    return loc;
  }

  @Override
  public String toString() {
    return node + " " + loc;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((loc == null) ? 0 : loc.hashCode());
    result = prime * result + ((node == null) ? 0 : node.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    GeneratedState other = (GeneratedState) obj;
    if (loc == null) {
      if (other.loc != null) return false;
    } else if (!loc.equals(other.loc)) return false;
    if (node == null) {
      if (other.node != null) return false;
    } else if (!node.equals(other.node)) return false;
    return true;
  }
}
