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
import typestate.TransitionFunction;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public class PipedInputStreamStateMachine extends TypeStateMachineWeightFunctions {

  private static final String CONNECT_METHODS = "connect";
  private static final String READ_METHODS = "read";

  public static enum States implements State {
    INIT,
    CONNECTED,
    ERROR;

    @Override
    public boolean isErrorState() {
      return this == ERROR;
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

  public PipedInputStreamStateMachine() {
    addTransition(
        new MatcherTransition(
            States.INIT, CONNECT_METHODS, Parameter.This, States.CONNECTED, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.INIT, READ_METHODS, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.CONNECTED, READ_METHODS, Parameter.This, States.CONNECTED, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.ERROR, READ_METHODS, Parameter.This, States.ERROR, Type.OnCall));
  }

  //    private Set<SootMethod> connect() {
  //        return selectMethodByName(getSubclassesOf("java.io.PipedInputStream"), "connect");
  //    }
  //
  //    private Set<SootMethod> readMethods() {
  //        return selectMethodByName(getSubclassesOf("java.io.PipedInputStream"), "read");
  //    }

  @Override
  public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(Edge unit) {
    return generateAtAllocationSiteOf(unit, java.io.PipedInputStream.class);
  }

  @Override
  protected State initialState() {
    return States.INIT;
  }
}
