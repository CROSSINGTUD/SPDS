package typestate.impl.statemachines.alloc;

import java.util.Collection;
import java.util.HashSet;
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

public class InputStreamStateMachine extends TypeStateMachineWeightFunctions{

	public static enum States implements State {
		 OPEN, CLOSED, ERROR;

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

	InputStreamStateMachine() {
		addTransition(
				new MatcherTransition(States.OPEN, closeMethods(), Parameter.This, States.CLOSED, Type.OnReturn));
		addTransition(
				new MatcherTransition(States.CLOSED, closeMethods(), Parameter.This, States.CLOSED, Type.OnReturn));
		addTransition(new MatcherTransition(States.OPEN, readMethods(), Parameter.This, States.OPEN, Type.OnReturn));
		addTransition(new MatcherTransition(States.ERROR, readMethods(), Parameter.This, States.ERROR, Type.OnReturn));
		addTransition(new MatcherTransition(States.CLOSED, readMethods(), Parameter.This, States.ERROR, Type.OnReturn));
		addTransition(new MatcherTransition(States.ERROR, closeMethods(), Parameter.This, States.ERROR, Type.OnReturn));

		addTransition(new MatcherTransition(States.CLOSED, nativeReadMethods(), Parameter.This, States.ERROR, Type.OnCallToReturn));
		addTransition(new MatcherTransition(States.ERROR, nativeReadMethods(), Parameter.This, States.ERROR, Type.OnCallToReturn));
		addTransition(new MatcherTransition(States.OPEN, nativeReadMethods(), Parameter.This, States.OPEN, Type.OnCallToReturn));
	}


	private Set<SootMethod> nativeReadMethods() {
		List<SootClass> subclasses = getSubclassesOf("java.io.InputStream");
		Set<SootMethod> out = new HashSet<>();
		for (SootClass c : subclasses) {
			for (SootMethod m : c.getMethods())
				if (m.isNative() && m.toString().contains("read()"))
					out.add(m);
		}
		return out;
	}


	private Set<SootMethod> constructors() {
		List<SootClass> subclasses = getSubclassesOf("java.io.InputStream");
		Set<SootMethod> out = new HashSet<>();
		for (SootClass c : subclasses) {
			for (SootMethod m : c.getMethods())
				if (m.isConstructor())
					out.add(m);
		}
		return out;
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
		return this.generateThisAtAnyCallSitesOf(method, unit, calledMethod, constructors());
	}

}
