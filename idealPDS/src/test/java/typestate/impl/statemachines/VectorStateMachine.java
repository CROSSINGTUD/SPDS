package typestate.impl.statemachines;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import boomerang.accessgraph.AccessGraph;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import typestate.ConcreteState;
import typestate.TypestateChangeFunction;
import typestate.TypestateDomainValue;
import typestate.finiteautomata.MatcherStateMachine;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;

public class VectorStateMachine extends MatcherStateMachine<ConcreteState> implements TypestateChangeFunction<ConcreteState> {

	public static enum States implements ConcreteState {
		INIT, NOT_EMPTY, ACCESSED_EMPTY;

		@Override
		public boolean isErrorState() {
			return this == ACCESSED_EMPTY;
		}

	}

	public VectorStateMachine() {
		addTransition(
				new MatcherTransition<ConcreteState>(States.INIT, addElement(), Parameter.This, States.NOT_EMPTY, Type.OnReturn));
		addTransition(new MatcherTransition<ConcreteState>(States.INIT, accessElement(), Parameter.This, States.ACCESSED_EMPTY,
				Type.OnReturn));
		addTransition(new MatcherTransition<ConcreteState>(States.NOT_EMPTY, accessElement(), Parameter.This, States.NOT_EMPTY,
				Type.OnReturn));

		addTransition(new MatcherTransition<ConcreteState>(States.NOT_EMPTY, removeAllElements(), Parameter.This, States.INIT,
				Type.OnReturn));
		addTransition(
				new MatcherTransition<ConcreteState>(States.INIT, removeAllElements(), Parameter.This, States.INIT, Type.OnReturn));
		addTransition(new MatcherTransition<ConcreteState>(States.ACCESSED_EMPTY, accessElement(), Parameter.This,
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
	public Collection<AccessGraph> generateSeed(SootMethod m, Unit unit,
			Collection<SootMethod> calledMethod) {
		if(m.toString().contains("<clinit>"))
			return Collections.emptySet();
		return generateAtAllocationSiteOf(unit,Vector.class);
	}
	
	@Override
	public TypestateDomainValue<ConcreteState> getBottomElement() {
		return new TypestateDomainValue<ConcreteState>(States.INIT);
	}

}
