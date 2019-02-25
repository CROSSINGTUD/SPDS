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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import boomerang.WeightedForwardQuery;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import typestate.TransitionFunction;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public class OutputStreamStateMachine extends TypeStateMachineWeightFunctions{


	public static enum States implements State {
		NONE, CLOSED, ERROR;

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

	public OutputStreamStateMachine() {
		addTransition(new MatcherTransition(States.NONE, closeMethods(), Parameter.This, States.CLOSED, Type.OnReturn));
		addTransition(
				new MatcherTransition(States.CLOSED, closeMethods(), Parameter.This, States.CLOSED, Type.OnReturn));
		addTransition(new MatcherTransition(States.CLOSED, writeMethods(), Parameter.This, States.ERROR, Type.OnReturn));
		addTransition(new MatcherTransition(States.ERROR, writeMethods(), Parameter.This, States.ERROR, Type.OnReturn));
	}


	private Set<SootMethod> closeMethods() {
		return selectMethodByName(getImplementersOf("java.io.OutputStream"), "close");
	}

	private Set<SootMethod> writeMethods() {
		return selectMethodByName(getImplementersOf("java.io.OutputStream"), "write");
	}


	private List<SootClass> getImplementersOf(String className) {
		SootClass sootClass = Scene.v().getSootClass(className);
		List<SootClass> list = Scene.v().getActiveHierarchy().getSubclassesOfIncluding(sootClass);
		List<SootClass> res = new LinkedList<>();
		for (SootClass c : list) {
			res.add(c);
		}
		return res;
	}

	@Override
	public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(SootMethod method, Unit unit) {
		return generateThisAtAnyCallSitesOf(method, unit,closeMethods());
	}

	@Override
	protected State initialState() {
		return States.NONE;
	}
}
