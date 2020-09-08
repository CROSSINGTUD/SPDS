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
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Statement;
import java.util.Collections;
import java.util.Set;
import typestate.TransitionFunction;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public class HasNextStateMachine extends TypeStateMachineWeightFunctions {

  private static String NEXT_METHOD = ".* next\\(\\)";
  private static String HAS_NEXT_METHOD = ".* hasNext\\(\\)";

  public static enum States implements State {
    NONE,
    INIT,
    HASNEXT,
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

  public HasNextStateMachine() {
    addTransition(
        new MatcherTransition(States.INIT, NEXT_METHOD, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.ERROR, NEXT_METHOD, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.HASNEXT, NEXT_METHOD, Parameter.This, States.INIT, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.INIT, HAS_NEXT_METHOD, Parameter.This, States.HASNEXT, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.HASNEXT, HAS_NEXT_METHOD, Parameter.This, States.HASNEXT, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.ERROR, HAS_NEXT_METHOD, Parameter.This, States.ERROR, Type.OnCall));
  }

  public Set<WeightedForwardQuery<TransitionFunction>> generateSeed(Edge edge) {
    Statement unit = edge.getStart();
    if (unit.containsInvokeExpr() && unit.isAssign()) {
      boomerang.scene.InvokeExpr invokeExpr = unit.getInvokeExpr();
      if (invokeExpr.isInstanceInvokeExpr()) {
        if (invokeExpr.getMethod().getName().contains("iterator")) {
          return Collections.singleton(
              new WeightedForwardQuery<>(
                  edge,
                  new AllocVal(unit.getLeftOp(), unit, unit.getLeftOp()),
                  initialTransition()));
        }
      }
    }
    return Collections.emptySet();
  }

  @Override
  public State initialState() {
    return States.INIT;
  }
}
