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
package typestate.impl.statemachines.alloc;

import java.util.Collection;
import java.util.HashSet;
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

public class PrintWriterStateMachine extends TypeStateMachineWeightFunctions{

	public static enum States implements State {
		OPEN, CLOSED, ERROR;

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

	public PrintWriterStateMachine() {
		addTransition(new MatcherTransition(States.OPEN, closeMethods(), Parameter.This, States.CLOSED,
				Type.OnReturn));
		addTransition(new MatcherTransition(States.CLOSED, closeMethods(), Parameter.This, States.CLOSED,
				Type.OnReturn));
		addTransition(new MatcherTransition(States.CLOSED, readMethods(), Parameter.This, States.ERROR,
				Type.OnReturn));
		addTransition(new MatcherTransition(States.ERROR, readMethods(), Parameter.This, States.ERROR,
				Type.OnReturn));
		addTransition(new MatcherTransition(States.ERROR, closeMethods(), Parameter.This, States.ERROR,
				Type.OnReturn));
		addTransition(new MatcherTransition(States.OPEN, readMethods(), Parameter.This, States.OPEN,
				Type.OnReturn));

	}

	private Set<SootMethod> closeMethods() {
		return selectMethodByName(getSubclassesOf("java.io.PrintWriter"), "close");
	}

	private Set<SootMethod> readMethods() {
		List<SootClass> subclasses = getSubclassesOf("java.io.PrintWriter");
		Set<SootMethod> closeMethods = closeMethods();
		Set<SootMethod> out = new HashSet<>();
		for (SootClass c : subclasses) {
			for (SootMethod m : c.getMethods())
				if (m.isPublic() && !closeMethods.contains(m) && !m.isStatic())
					out.add(m);
		}
		return out;
	}

	@Override
	public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(SootMethod m, Unit unit, Collection<SootMethod> calledMethod) {
		return generateAtAllocationSiteOf(m, unit, java.io.PrintWriter.class);
	}

	@Override
	protected State initialState() {
		return States.OPEN;
	}

}
