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

import boomerang.scene.Pair;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import java.util.Set;
import wpds.impl.Weight;
import wpds.interfaces.State;

public class IgnoreArrayStrategy<W extends Weight> implements ArrayHandlingStrategy<W> {

  @Override
  public void handleForward(
      Statement storeStmt,
      Pair<Val, Integer> storedVal,
      Set<State> out,
      ForwardBoomerangSolver<W> solver) {}

  @Override
  public void handleBackward(
      Statement curr,
      Pair<Val, Integer> arrayBase,
      Statement succ,
      Set<State> out,
      BackwardBoomerangSolver<W> solver) {}
}
