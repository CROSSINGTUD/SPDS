/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package typestate.impl.statemachines;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import boomerang.WeightedForwardQuery;
import soot.SootMethod;
import soot.Unit;
import typestate.TransitionFunction;
import typestate.finiteautomata.*;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;

public class FileMustBeClosedStateMachine extends TypeStateMachineWeightFunctions{

  public static enum States implements State {
    INIT, OPENED, CLOSED;

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
    addTransition(new MatcherTransition(States.INIT, ".*open.*",Parameter.This, States.OPENED, Type.OnCall));
    addTransition(new MatcherTransition(States.INIT, ".*close.*",Parameter.This, States.CLOSED, Type.OnCall));
    addTransition(new MatcherTransition(States.OPENED, ".*close.*",Parameter.This, States.CLOSED, Type.OnCall));
  }


    @Override
    public State initialState() {
        return States.INIT;
    }

    @Override
  public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(SootMethod method, Unit unit,
                                                                    Collection<SootMethod> calledMethod) {
    try {
		return generateAtAllocationSiteOf(method, unit, Class.forName("typestate.test.helper.File"));
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
	}
    return Collections.emptySet();
  }
}
