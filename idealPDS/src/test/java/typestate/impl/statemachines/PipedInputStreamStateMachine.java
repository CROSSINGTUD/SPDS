package typestate.impl.statemachines;

import java.util.Collection;
import java.util.Set;

import boomerang.jimple.AllocVal;
import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Unit;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;

public class PipedInputStreamStateMachine extends TypeStateMachineWeightFunctions {

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

		@Override
		public boolean isAccepting() {
			return false;
		}
	}

	public 	PipedInputStreamStateMachine() {
		addTransition(
				new MatcherTransition(States.INIT, connect(), Parameter.This, States.CONNECTED, Type.OnReturn));
		addTransition(new MatcherTransition(States.INIT, readMethods(), Parameter.This, States.ERROR, Type.OnReturn));
		addTransition(new MatcherTransition(States.CONNECTED, readMethods(), Parameter.This, States.CONNECTED, Type.OnReturn));
		addTransition(new MatcherTransition(States.ERROR, readMethods(), Parameter.This, States.ERROR, Type.OnReturn));
	}

	private Set<SootMethod> connect() {
		return selectMethodByName(getSubclassesOf("java.io.PipedInputStream"), "connect");
	}


	private Set<SootMethod> readMethods() {
		return selectMethodByName(getSubclassesOf("java.io.PipedInputStream"), "read");
	}


	@Override
	public Collection<AllocVal> generateSeed(SootMethod m, Unit unit,
			Collection<SootMethod> calledMethod) {
		return generateAtAllocationSiteOf(m, unit, java.io.PipedInputStream.class);
	}
}
