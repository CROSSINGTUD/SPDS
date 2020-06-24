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

import wpds.interfaces.State;

public class Node<Stmt, Fact> implements State {

  protected final Stmt stmt;
  protected final Fact variable;
  private int hashCode;

  public Node(Stmt stmt, Fact variable) {
    this.stmt = stmt;
    this.variable = variable;
  }

  public Stmt stmt() {
    return stmt;
  }

  public Fact fact() {
    return variable;
  }

  @Override
  public int hashCode() {
    if (hashCode != 0) return hashCode;
    final int prime = 31;
    int result = 1;
    result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
    result = prime * result + ((variable == null) ? 0 : variable.hashCode());
    hashCode = result;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Node other = (Node) obj;
    if (stmt == null) {
      if (other.stmt != null) return false;
    } else if (!stmt.equals(other.stmt)) return false;
    if (variable == null) {
      if (other.variable != null) return false;
    } else if (!variable.equals(other.variable)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "(" + variable + "," + stmt + ")";
  }
}
