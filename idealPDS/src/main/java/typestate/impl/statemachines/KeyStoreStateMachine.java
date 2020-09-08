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
import boomerang.scene.DeclaredMethod;
import boomerang.scene.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import soot.SootClass;
import soot.SootMethod;
import typestate.TransitionFunction;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public class KeyStoreStateMachine extends TypeStateMachineWeightFunctions {

  private static String LOAD_METHOD = ".* load.*";

  public static enum States implements State {
    NONE,
    INIT,
    LOADED,
    ERROR;

    @Override
    public boolean isErrorState() {
      return this == ERROR;
    }

    @Override
    public boolean isInitialState() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isAccepting() {
      return false;
    }
  }

  public KeyStoreStateMachine() {
    // addTransition(new MatcherTransition(States.NONE,
    // keyStoreConstructor(),Parameter.This, States.INIT, Type.OnReturn));
    addTransition(
        new MatcherTransition(
            States.INIT, LOAD_METHOD, Parameter.This, States.LOADED, Type.OnCallToReturn));
    addTransition(
        new MatcherTransition(
            States.LOADED, LOAD_METHOD, true, Parameter.This, States.LOADED, Type.OnCallToReturn));

    addTransition(
        new MatcherTransition(
            States.INIT, LOAD_METHOD, true, Parameter.This, States.ERROR, Type.OnCallToReturn));
    addTransition(
        new MatcherTransition(
            States.ERROR, LOAD_METHOD, true, Parameter.This, States.ERROR, Type.OnCallToReturn));
  }

  private Set<SootMethod> keyStoreConstructor() {
    List<SootClass> subclasses = getSubclassesOf("java.security.KeyStore");
    Set<SootMethod> out = new HashSet<>();
    for (SootClass c : subclasses) {
      for (SootMethod m : c.getMethods())
        if (m.getName().equals("getInstance") && m.isStatic()) out.add(m);
    }
    return out;
  }

  @Override
  public Set<WeightedForwardQuery<TransitionFunction>> generateSeed(Edge edge) {
    Statement unit = edge.getStart();
    if (unit.isAssign() && unit.containsInvokeExpr()) {
      if (isKeyStoreConstructor(unit.getInvokeExpr().getMethod())) {
        return Collections.singleton(
            new WeightedForwardQuery<>(
                edge,
                new AllocVal(unit.getLeftOp(), unit, unit.getRightOp()),
                initialTransition()));
      }
    }
    return Collections.emptySet();
  }

  private boolean isKeyStoreConstructor(DeclaredMethod method) {
    return method.getName().equals("getInstance") && method.getSubSignature().contains("KeyStore");
  }

  @Override
  protected State initialState() {
    return States.INIT;
  }
}
