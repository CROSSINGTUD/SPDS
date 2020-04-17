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
package test;

import boomerang.scene.Statement;
import boomerang.scene.Val;
import com.google.common.collect.Sets;
import java.util.Set;
import typestate.TransitionFunction;
import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.State;
import typestate.finiteautomata.Transition;

public class MustBe extends ExpectedResults<TransitionFunction, Val> {

  MustBe(Statement unit, Val val, InternalState state) {
    super(unit, val, state);
  }

  public String toString() {
    return "MustBe " + super.toString();
  }

  @Override
  public void computedResults(TransitionFunction val) {
    Set<State> states = Sets.newHashSet();
    for (ITransition t : val.values()) {
      if (!t.equals(Transition.identity())) {
        states.add(t.to());
      }
    }
    for (State s : states) {
      if (state == InternalState.ACCEPTING) {
        satisfied |= !s.isErrorState() && states.size() == 1;
      } else if (state == InternalState.ERROR) {
        satisfied |= s.isErrorState() && states.size() == 1;
      }
    }
  }
}
