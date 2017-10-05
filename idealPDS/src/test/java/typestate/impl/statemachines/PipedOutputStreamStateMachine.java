package typestate.impl.statemachines;

import java.util.Collection;
import java.util.Set;

import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Unit;
import typestate.finiteautomata.MatcherStateMachine;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;

public class PipedOutputStreamStateMachine extends MatcherStateMachine{


	public static enum States implements State {
		 INIT, CONNECTED, ERROR;

		@Override
		public boolean isErrorState() {
			return this == ERROR;
		}

		@Override
		public boolean isInitialState() {
			return false;
		}

	}

	PipedOutputStreamStateMachine() {
		addTransition(
				new MatcherTransition(States.INIT, connect(), Parameter.This, States.CONNECTED, Type.OnReturn));
		addTransition(new MatcherTransition(States.INIT, readMethods(), Parameter.This, States.ERROR, Type.OnReturn));
		addTransition(new MatcherTransition(States.CONNECTED, readMethods(), Parameter.This, States.CONNECTED, Type.OnReturn));
		addTransition(new MatcherTransition(States.ERROR, readMethods(), Parameter.This, States.ERROR, Type.OnReturn));
	}
	private Set<SootMethod> connect() {
		return selectMethodByName(getSubclassesOf("java.io.PipedOutputStream"), "connect");
	}


	private Set<SootMethod> readMethods() {
		return selectMethodByName(getSubclassesOf("java.io.PipedOutputStream"), "write");
	}


	@Override
	public Collection<Val> generateSeed(SootMethod m, Unit unit,
			Collection<SootMethod> calledMethod) {
		return generateAtAllocationSiteOf(m, unit, java.io.PipedOutputStream.class);
	}
}
