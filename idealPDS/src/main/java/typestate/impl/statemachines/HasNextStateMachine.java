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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import boomerang.WeightedForwardQuery;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import typestate.TransitionFunction;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;

public class HasNextStateMachine extends TypeStateMachineWeightFunctions {

	private Set<SootMethod> hasNextMethods;
	private HashSet<SootMethod> res;

	public static enum States implements State {
		NONE, INIT, HASNEXT, ERROR;

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
				new MatcherTransition(States.INIT, retrieveNextMethods(), Parameter.This, States.ERROR, Type.OnReturn));
		addTransition(new MatcherTransition(States.ERROR, retrieveNextMethods(), Parameter.This, States.ERROR,
				Type.OnReturn));
		addTransition(new MatcherTransition(States.HASNEXT, retrieveNextMethods(), Parameter.This, States.INIT,
				Type.OnReturn));
		addTransition(new MatcherTransition(States.INIT, retrieveHasNextMethods(), Parameter.This, States.HASNEXT,
				Type.OnReturn));
		addTransition(new MatcherTransition(States.HASNEXT, retrieveHasNextMethods(), Parameter.This, States.HASNEXT,
				Type.OnReturn));
		addTransition(new MatcherTransition(States.ERROR, retrieveHasNextMethods(), Parameter.This, States.ERROR,
				Type.OnReturn));
	}

	private Set<SootMethod> retrieveHasNextMethods() {
		if (hasNextMethods == null)
			hasNextMethods = selectMethodByName(getImplementersOfIterator("java.util.Iterator"), "hasNext");
		return hasNextMethods;
	}

	private Set<SootMethod> retrieveNextMethods() {
		return selectMethodByName(getImplementersOfIterator("java.util.Iterator"), "next");
	}

	private Set<SootMethod> retrieveIteratorConstructors() {
		if(res != null)
			return res;
		Set<SootMethod> selectMethodByName = selectMethodByName(Scene.v().getClasses(), "iterator");
		res = new HashSet<>();
		for (SootMethod m : selectMethodByName) {
			if (m.getReturnType() instanceof RefType) {
				RefType refType = (RefType) m.getReturnType();
				SootClass classs = refType.getSootClass();
				if (classs.equals(Scene.v().getSootClass("java.util.Iterator")) || Scene.v().getActiveHierarchy()
						.getImplementersOf(Scene.v().getSootClass("java.util.Iterator")).contains(classs)) {
					res.add(m);
				}
			}
		}
		return res;
	}

	private List<SootClass> getImplementersOfIterator(String className) {
		SootClass sootClass = Scene.v().getSootClass(className);
		List<SootClass> list = Scene.v().getActiveHierarchy().getImplementersOf(sootClass);
		List<SootClass> res = new LinkedList<>();
		for (SootClass c : list) {
			res.add(c);
		}
		return res;
	}

	@Override
	public Set<WeightedForwardQuery<TransitionFunction>> generateSeed(SootMethod method, Unit unit, Collection<SootMethod> calledMethod) {
		for (SootMethod m : calledMethod) {
			if (retrieveIteratorConstructors().contains(m)) {
				if (unit instanceof AssignStmt) {
					AssignStmt stmt = (AssignStmt) unit;
					return Collections.singleton(new WeightedForwardQuery<>(new Statement(stmt,method),new AllocVal(stmt.getLeftOp(), method, stmt.getRightOp()),initialTransition()));
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
