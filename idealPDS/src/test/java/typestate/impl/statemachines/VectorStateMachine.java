package typestate.impl.statemachines;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import boomerang.jimple.Val;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import typestate.finiteautomata.MatcherStateMachine;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;

public class VectorStateMachine extends MatcherStateMachine {

	public static enum States implements State {
		INIT, NOT_EMPTY, ACCESSED_EMPTY;

		@Override
		public boolean isErrorState() {
			return this == ACCESSED_EMPTY;
		}

		@Override
		public boolean isInitialState() {
			return false;
		}

	}

	public VectorStateMachine() {
		addTransition(
				new MatcherTransition(States.INIT, addElement(), Parameter.This, States.NOT_EMPTY, Type.OnReturn));
		addTransition(new MatcherTransition(States.INIT, accessElement(), Parameter.This, States.ACCESSED_EMPTY,
				Type.OnReturn));
		addTransition(new MatcherTransition(States.NOT_EMPTY, accessElement(), Parameter.This, States.NOT_EMPTY,
				Type.OnReturn));

		addTransition(new MatcherTransition(States.NOT_EMPTY, removeAllElements(), Parameter.This, States.INIT,
				Type.OnReturn));
		addTransition(
				new MatcherTransition(States.INIT, removeAllElements(), Parameter.This, States.INIT, Type.OnReturn));
		addTransition(new MatcherTransition(States.ACCESSED_EMPTY, accessElement(), Parameter.This,
				States.ACCESSED_EMPTY, Type.OnReturn));
	}

	private Set<SootMethod> removeAllElements() {
		List<SootClass> vectorClasses = getSubclassesOf("java.util.Vector");
		Set<SootMethod> selectMethodByName = selectMethodByName(vectorClasses, "removeAllElements");
		return selectMethodByName;
	}

	private Set<SootMethod> addElement() {
		List<SootClass> vectorClasses = getSubclassesOf("java.util.Vector");
		Set<SootMethod> selectMethodByName = selectMethodByName(vectorClasses,
				"add|addAll|addElement|insertElementAt|set|setElementAt");
		return selectMethodByName;
	}

	private Set<SootMethod> accessElement() {
		List<SootClass> vectorClasses = getSubclassesOf("java.util.Vector");
		Set<SootMethod> selectMethodByName = selectMethodByName(vectorClasses,
				"elementAt|firstElement|lastElement|get");
		return selectMethodByName;
	}

	@Override
	public Collection<Val> generateSeed(SootMethod m, Unit unit, Collection<SootMethod> calledMethod) {
		if (m.toString().contains("<clinit>"))
			return Collections.emptySet();
		return generateAtAllocationSiteOf(m, unit, Vector.class);
	}

}
