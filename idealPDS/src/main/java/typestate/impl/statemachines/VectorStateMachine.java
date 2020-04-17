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
import boomerang.scene.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;
import typestate.TransitionFunction;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public class VectorStateMachine extends TypeStateMachineWeightFunctions {

  private static String ADD_ELEMENT_METHODS =
      "(.* (add|addAll|addElement|insertElementAt|set|setElementAt).*)|<java.util.Vector: void <init>(java.util.Collection)>";
  private static String ACCESS_ELEMENT_METHODS = ".* (elementAt|firstElement|lastElement|get).*";
  private static String REMOVE_ALL_METHODS = ".* removeAllElements.*";

  public static enum States implements State {
    INIT,
    NOT_EMPTY,
    ACCESSED_EMPTY;

    @Override
    public boolean isErrorState() {
      return this == ACCESSED_EMPTY;
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

  public VectorStateMachine() {
    addTransition(
        new MatcherTransition(
            States.INIT, ADD_ELEMENT_METHODS, Parameter.This, States.NOT_EMPTY, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.INIT,
            ACCESS_ELEMENT_METHODS,
            Parameter.This,
            States.ACCESSED_EMPTY,
            Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.NOT_EMPTY,
            ACCESS_ELEMENT_METHODS,
            Parameter.This,
            States.NOT_EMPTY,
            Type.OnCall));

    addTransition(
        new MatcherTransition(
            States.NOT_EMPTY, REMOVE_ALL_METHODS, Parameter.This, States.INIT, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.INIT, REMOVE_ALL_METHODS, Parameter.This, States.INIT, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.ACCESSED_EMPTY,
            ACCESS_ELEMENT_METHODS,
            Parameter.This,
            States.ACCESSED_EMPTY,
            Type.OnCall));
  }

  @Override
  public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(Statement unit) {
    if (unit.getMethod().toString().contains("<clinit>")) return Collections.emptySet();
    return generateAtAllocationSiteOf(unit, Vector.class);
  }

  @Override
  protected State initialState() {
    return States.INIT;
  }
}
