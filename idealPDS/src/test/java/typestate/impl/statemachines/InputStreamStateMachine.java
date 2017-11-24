package typestate.impl.statemachines;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import boomerang.jimple.AllocVal;
import boomerang.jimple.Val;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;

public class InputStreamStateMachine extends TypeStateMachineWeightFunctions {

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
		return selectMethodByName(getImplementersOf("java.io.InputStream"), "close");
	}

	private Set<SootMethod> readMethods() {
		return selectMethodByName(getImplementersOf("java.io.InputStream"), "read");
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
	public Collection<AllocVal> generateSeed(SootMethod method, Unit unit,
			Collection<SootMethod> calledMethod) {
		return this.generateThisAtAnyCallSitesOf(method, unit, calledMethod, closeMethods());
	}
}
