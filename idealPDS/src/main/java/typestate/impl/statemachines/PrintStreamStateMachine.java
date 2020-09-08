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

public class PrintStreamStateMachine extends TypeStateMachineWeightFunctions {

  public static enum States implements State {
    NONE,
    CLOSED,
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

  private static final String CLOSE_METHODS = ".* close.*";
  private static final String READ_METHODS = ".* (read|flush|write).*";
  private static final String TYPE = "java.io.PrintStream";

  public PrintStreamStateMachine() {
    addTransition(
        new MatcherTransition(
            States.CLOSED, CLOSE_METHODS, Parameter.This, States.CLOSED, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.CLOSED, READ_METHODS, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.ERROR, READ_METHODS, Parameter.This, States.ERROR, Type.OnCall));
    addTransition(
        new MatcherTransition(
            States.ERROR, CLOSE_METHODS, Parameter.This, States.ERROR, Type.OnCall));
  }

  //
  //    private Set<SootMethod> readMethods() {
  //        List<SootClass> subclasses = getSubclassesOf("java.io.PrintStream");
  //        Set<SootMethod> closeMethods = closeMethods();
  //        Set<SootMethod> out = new HashSet<>();
  //        for (SootClass c : subclasses) {
  //            for (SootMethod m : c.getMethods())
  //                if (m.isPublic() && !closeMethods.contains(m) && !m.isStatic())
  //                    out.add(m);
  //        }
  //        return out;
  //    }

  @Override
  public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(Edge unit) {
    return this.generateThisAtAnyCallSitesOf(unit, TYPE, CLOSE_METHODS);
  }

  @Override
  protected State initialState() {
    return States.CLOSED;
  }
}
