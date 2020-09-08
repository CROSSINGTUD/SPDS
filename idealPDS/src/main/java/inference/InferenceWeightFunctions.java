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
package inference;

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Val;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;

public class InferenceWeightFunctions implements WeightFunctions<Edge, Val, Edge, InferenceWeight> {

  @Override
  public InferenceWeight push(Node<Edge, Val> curr, Node<Edge, Val> succ, Edge field) {
    Method callee = succ.stmt().getMethod();
    if (!callee.isStatic()) {
      Val thisLocal = callee.getThisLocal();
      if (succ.fact().equals(thisLocal)) {
        return new InferenceWeight(callee);
      }
    }
    return getOne();
  }

  @Override
  public InferenceWeight normal(Node<Edge, Val> curr, Node<Edge, Val> succ) {
    return getOne();
  }

  @Override
  public InferenceWeight pop(Node<Edge, Val> curr) {
    return getOne();
  }

  @Override
  public InferenceWeight getOne() {
    return InferenceWeight.one();
  }
}
