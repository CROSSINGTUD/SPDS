package typestate.impl.statemachines;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import boomerang.jimple.Val;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;

public class HasNextStateMachine extends TypeStateMachineWeightFunctions {

	private Set<SootMethod> hasNextMethods;

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

	}

	public HasNextStateMachine() {
		addTransition(new MatcherTransition(States.NONE, retrieveIteratorConstructors(), Parameter.This, States.INIT,
				Type.None));
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
		Set<SootMethod> selectMethodByName = selectMethodByName(Scene.v().getClasses(), "iterator");
		Set<SootMethod> res = new HashSet<>();
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
	public Collection<Val> generateSeed(SootMethod method, Unit unit, Collection<SootMethod> calledMethod) {
		for (SootMethod m : calledMethod) {
			if (retrieveIteratorConstructors().contains(m)) {
				if (unit instanceof AssignStmt) {
					Set<Val> out = new HashSet<>();
					AssignStmt stmt = (AssignStmt) unit;
					out.add(new Val(stmt.getLeftOp(), method));
					return out;
				}
			}
		}

		return Collections.emptySet();
	}

}
