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
import java.util.List;
import java.util.Set;

import boomerang.WeightedForwardQuery;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import typestate.TransitionFunction;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;

public class URLConnStateMachine extends TypeStateMachineWeightFunctions{

	public static enum States implements State {
		NONE, INIT, CONNECTED, ERROR;

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

	public URLConnStateMachine() {
		addTransition(new MatcherTransition(States.CONNECTED, illegalOpertaion(), Parameter.This, States.ERROR,
				Type.OnReturn));
		addTransition(
				new MatcherTransition(States.ERROR, illegalOpertaion(), Parameter.This, States.ERROR, Type.OnReturn));
	}

	private Set<SootMethod> connect() {
		return selectMethodByName(getSubclassesOf("java.net.URLConnection"), "connect");
	}

	private Set<SootMethod> illegalOpertaion() {
		List<SootClass> subclasses = getSubclassesOf("java.net.URLConnection");
		return selectMethodByName(subclasses,
				"setDoInput|setDoOutput|setAllowUserInteraction|setUseCaches|setIfModifiedSince|setRequestProperty|addRequestProperty|getRequestProperty|getRequestProperties");
	}

	@Override
	public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(SootMethod m, Unit unit, Collection<SootMethod> calledMethod) {
		return this.generateThisAtAnyCallSitesOf(m, unit, calledMethod, connect());
	}

	@Override
	protected State initialState() {
		return States.CONNECTED;
	}
}
