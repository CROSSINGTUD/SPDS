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
import boomerang.scene.Statement;
import java.util.Collection;
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

public class SignatureStateMachine extends TypeStateMachineWeightFunctions {

  private static final String INIT_SIGN = "initSign";
  private static final String INIT_VERIFY = "initVerify";
  private static final String SIGN = "sign";
  private static final String UPDATE = "update";
  private static final String VERIFY = "verify";
  private static final String GET_INSTANCE = "getInstance";

  public static enum States implements State {
    NONE,
    UNITIALIZED,
    SIGN_CHECK,
    VERIFY_CHECK,
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

  public SignatureStateMachine() {
    addTransition(
        new MatcherTransition(
            States.NONE, GET_INSTANCE, Parameter.This, States.UNITIALIZED, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.UNITIALIZED, INIT_SIGN, Parameter.This, States.SIGN_CHECK, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.UNITIALIZED, INIT_VERIFY, Parameter.This, States.VERIFY_CHECK, Type.OnCall));
    addTransition(
        new MatcherTransition(States.UNITIALIZED, SIGN, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.UNITIALIZED, VERIFY, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.UNITIALIZED, UPDATE, Parameter.This, States.ERROR, Type.OnCall));

    addTransition(
        new MatcherTransition(
            States.SIGN_CHECK, INIT_SIGN, Parameter.This, States.SIGN_CHECK, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.SIGN_CHECK, INIT_VERIFY, Parameter.This, States.VERIFY_CHECK, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.SIGN_CHECK, SIGN, Parameter.This, States.SIGN_CHECK, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.SIGN_CHECK, VERIFY, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.SIGN_CHECK, UPDATE, Parameter.This, States.SIGN_CHECK, Type.OnCall));

    addTransition(
        new MatcherTransition(
            States.VERIFY_CHECK, INIT_SIGN, Parameter.This, States.SIGN_CHECK, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.VERIFY_CHECK, INIT_VERIFY, Parameter.This, States.VERIFY_CHECK, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.VERIFY_CHECK, SIGN, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.VERIFY_CHECK, VERIFY, Parameter.This, States.VERIFY_CHECK, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.VERIFY_CHECK, UPDATE, Parameter.This, States.VERIFY_CHECK, Type.OnCall));

    addTransition(
        new MatcherTransition(States.ERROR, INIT_SIGN, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.ERROR, INIT_VERIFY, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(States.ERROR, SIGN, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(States.ERROR, VERIFY, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(States.ERROR, UPDATE, Parameter.This, States.ERROR, Type.OnCall));
  }

  private Set<SootMethod> constructor() {
    List<SootClass> subclasses = getSubclassesOf("java.security.Signature");
    Set<SootMethod> out = new HashSet<>();
    for (SootClass c : subclasses) {
      for (SootMethod m : c.getMethods())
        if (m.isPublic() && m.getName().equals("getInstance")) out.add(m);
    }
    return out;
  }

  @Override
  public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(Edge edge) {
    Statement unit = edge.getStart();
    if (unit.containsInvokeExpr()) {
      DeclaredMethod method = unit.getInvokeExpr().getMethod();
      if (method.getName().equals("getInstance")
          && method.getSubSignature().contains("Signature")) {
        return getLeftSideOf(edge);
      }
    }
    return Collections.emptySet();
  }

  @Override
  protected State initialState() {
    return States.UNITIALIZED;
  }
}
