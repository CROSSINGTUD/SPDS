/**
 * ***************************************************************************** Copyright (c) 2020
 * CodeShield GmbH, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.arrays;

import boomerang.scene.Field;
import boomerang.scene.Pair;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import java.util.Set;
import sync.pds.solver.SyncPDSSolver.PDSSystem;
import sync.pds.solver.nodes.PushNode;
import wpds.impl.Weight;
import wpds.interfaces.State;

public class ArrayIndexSensitiveStrategy<W extends Weight> implements ArrayHandlingStrategy<W> {

  @Override
  public void handleForward(
      Statement curr,
      Pair<Val, Integer> arrayBase,
      Set<State> out,
      ForwardBoomerangSolver<W> solver) {
    out.add(
        new PushNode<>(curr, arrayBase.getX(), Field.array(arrayBase.getY()), PDSSystem.FIELDS));
  }

  @Override
  public void handleBackward(
      Statement curr,
      Pair<Val, Integer> arrayBase,
      Statement succ,
      Set<State> out,
      BackwardBoomerangSolver<W> solver) {
    out.add(
        new PushNode<>(succ, arrayBase.getX(), Field.array(arrayBase.getY()), PDSSystem.FIELDS));
  }
}
