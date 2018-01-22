/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package typestate.impl.statemachines;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import boomerang.WeightedForwardQuery;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import typestate.TransitionFunction;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;

public class InputStreamStateMachine extends TypeStateMachineWeightFunctions {

	private Set<SootMethod> closeMethods;
	private Set<SootMethod> readMethods;

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

	public InputStreamStateMachine() {
		addTransition(new MatcherTransition(States.NONE, closeMethods(), Parameter.This, States.CLOSED, Type.OnReturn));
		addTransition(
				new MatcherTransition(States.CLOSED, closeMethods(), Parameter.This, States.CLOSED, Type.OnReturn));
		addTransition(new MatcherTransition(States.CLOSED, readMethods(), Parameter.This, States.ERROR, Type.OnReturn));
		addTransition(new MatcherTransition(States.ERROR, readMethods(), Parameter.This, States.ERROR, Type.OnReturn));

		addTransition(new MatcherTransition(States.CLOSED, Collections.singleton(Scene.v().getMethod("<java.io.InputStream: int read()>")), Parameter.This, States.ERROR, Type.OnCallToReturn));
		addTransition(new MatcherTransition(States.ERROR, Collections.singleton(Scene.v().getMethod("<java.io.InputStream: int read()>")), Parameter.This, States.ERROR, Type.OnCallToReturn));
	}
	private Set<SootMethod> closeMethods() {
		if(closeMethods == null)
			closeMethods = selectMethodByName(getImplementersOf("java.io.InputStream"), "close");
		return closeMethods;
	}

	private Set<SootMethod> readMethods() {
		if(readMethods == null){
			readMethods = selectMethodByName(getImplementersOf("java.io.InputStream"), "read");
		}

		return readMethods;
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
	public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(SootMethod method, Unit unit,
																	  Collection<SootMethod> calledMethod) {
		return this.generateThisAtAnyCallSitesOf(method, unit, calledMethod, closeMethods());
	}

	@Override
	public State initialState() {
		return States.NONE;
	}
}
