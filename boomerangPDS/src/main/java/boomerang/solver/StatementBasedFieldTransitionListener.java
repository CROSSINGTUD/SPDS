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

import boomerang.scene.Field;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAUpdateListener;

public abstract class StatementBasedFieldTransitionListener<W extends Weight>
    implements WPAUpdateListener<Field, INode<Node<Statement, Val>>, W> {

  private final Statement stmt;

  public StatementBasedFieldTransitionListener(Statement stmt) {
    this.stmt = stmt;
  }

  public Statement getStmt() {
    return stmt;
  }

  @Override
  public void onWeightAdded(
      Transition<Field, INode<Node<Statement, Val>>> t,
      W w,
      WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
    onAddedTransition(t);
  }

  public abstract void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t);

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    StatementBasedFieldTransitionListener other = (StatementBasedFieldTransitionListener) obj;
    if (stmt == null) {
      if (other.stmt != null) return false;
    } else if (!stmt.equals(other.stmt)) return false;
    return true;
  }
}
