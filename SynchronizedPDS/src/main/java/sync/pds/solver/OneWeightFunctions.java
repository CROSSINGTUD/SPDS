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
import wpds.impl.Weight;

public class OneWeightFunctions<Stmt, Fact, Field, W extends Weight>
    implements WeightFunctions<Stmt, Fact, Field, W> {
  private W one;

  public OneWeightFunctions(W one) {
    this.one = one;
  }

  @Override
  public W push(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ, Field field) {
    return one;
  }

  @Override
  public W normal(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ) {
    return one;
  }

  @Override
  public W pop(Node<Stmt, Fact> curr) {
    return one;
  }

  @Override
  public W getOne() {
    return one;
  }
};
