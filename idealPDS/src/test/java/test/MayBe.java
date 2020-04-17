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
import typestate.TransitionFunction;
import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.State;

public class MayBe extends ExpectedResults<TransitionFunction, Val> {

  MayBe(Statement unit, Val accessGraph, InternalState state) {
    super(unit, accessGraph, state);
  }

  public String toString() {
    return "Maybe " + super.toString();
  }

  @Override
  public void computedResults(TransitionFunction results) {
    for (ITransition t : results.values()) {
      // if(t.equals(Transition.identity()))
      // continue;
      State s = t.to();
      if (s != null)
        if (state == InternalState.ACCEPTING) {
          satisfied |= !s.isErrorState();
        } else if (state == InternalState.ERROR) {
          satisfied |= s.isErrorState();
        }
    }
  }
}
