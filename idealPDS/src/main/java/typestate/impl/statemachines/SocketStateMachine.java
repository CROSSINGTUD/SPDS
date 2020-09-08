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
import boomerang.scene.DeclaredMethod;
import java.net.Socket;
import java.util.Collection;
import typestate.TransitionFunction;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public class SocketStateMachine extends TypeStateMachineWeightFunctions {

  private static String CONNECT_METHOD = ".* connect.*";

  public enum States implements State {
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

  public SocketStateMachine() {
    addTransition(
        new MatcherTransition(
            States.INIT,
            CONNECT_METHOD,
            Parameter.This,
            States.CONNECTED,
            Type.OnCallOrOnCallToReturn));
    addTransition(
        new MatcherTransition(
            States.ERROR,
            CONNECT_METHOD,
            Parameter.This,
            States.ERROR,
            Type.OnCallOrOnCallToReturn));
    addTransition(
        new UseMethodMatcher(
            States.CONNECTED, Parameter.This, States.CONNECTED, Type.OnCallOrOnCallToReturn));
    addTransition(
        new UseMethodMatcher(
            States.INIT, Parameter.This, States.ERROR, Type.OnCallOrOnCallToReturn));
    addTransition(
        new MatcherTransition(
            States.CONNECTED,
            CONNECT_METHOD,
            Parameter.This,
            States.CONNECTED,
            Type.OnCallOrOnCallToReturn));
    addTransition(
        new UseMethodMatcher(
            States.ERROR, Parameter.This, States.ERROR, Type.OnCallOrOnCallToReturn));
  }

  @Override
  public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(Edge unit) {
    return generateAtAllocationSiteOf(unit, Socket.class);
  }

  @Override
  protected State initialState() {
    return States.INIT;
  }

  private static class UseMethodMatcher extends MatcherTransition {

    public UseMethodMatcher(State from, Parameter param, State to, Type type) {
      super(from, CONNECT_METHOD, param, to, type);
    }

    @Override
    public boolean matches(DeclaredMethod declaredMethod) {
      if (super.matches(declaredMethod)) {
        return false;
      }
      boolean isSocketMethod =
          declaredMethod.getDeclaringClass().getFullyQualifiedName().equals("java.net.Socket");
      if (!isSocketMethod) {
        return false;
      }
      String methodName = declaredMethod.getName();
      boolean isConstructor = methodName.contains("<init>");
      boolean isSetImpl = methodName.startsWith("setImpl");
      return !(isConstructor || isSetImpl);
    }
  }
}
