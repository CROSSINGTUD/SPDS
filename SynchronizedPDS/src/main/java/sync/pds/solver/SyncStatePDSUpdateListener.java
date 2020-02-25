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
package sync.pds.solver;

import sync.pds.solver.nodes.Node;
import wpds.interfaces.Location;

public abstract class SyncStatePDSUpdateListener<Stmt extends Location, Fact> {

  private Node<Stmt, Fact> node;

  public SyncStatePDSUpdateListener(Node<Stmt, Fact> node) {
    this.node = node;
  }

  public abstract void reachable();

  public Node<Stmt, Fact> getNode() {
    return node;
  }
}
