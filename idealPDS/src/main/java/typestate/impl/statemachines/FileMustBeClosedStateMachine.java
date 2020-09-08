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
package typestate.impl.statemachines;

import boomerang.WeightedForwardQuery;
import boomerang.scene.ControlFlowGraph.Edge;
import java.util.Collection;
import java.util.Collections;
import typestate.TransitionFunction;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public class FileMustBeClosedStateMachine extends TypeStateMachineWeightFunctions {

  public static enum States implements State {
    INIT,
    OPENED,
    CLOSED;

    @Override
    public boolean isErrorState() {
      return this == OPENED;
    }

    @Override
    public boolean isInitialState() {
      return false;
    }

    @Override
    public boolean isAccepting() {
      return false;
    }
  }

  public FileMustBeClosedStateMachine() {
    addTransition(
        new MatcherTransition(States.INIT, ".*open.*", Parameter.This, States.OPENED, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.INIT, ".*close.*", Parameter.This, States.CLOSED, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.OPENED, ".*close.*", Parameter.This, States.CLOSED, Type.OnCall));
  }

  @Override
  public State initialState() {
    return States.INIT;
  }

  @Override
  public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(Edge unit) {
    try {
      return generateAtAllocationSiteOf(unit, Class.forName("typestate.test.helper.File"));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    return Collections.emptySet();
  }
}
