package typestate.impl.statemachines;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import boomerang.jimple.Val;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;

public class KeyStoreStateMachine extends TypeStateMachineWeightFunctions{

	public static enum States implements State {
		NONE, INIT, LOADED, ERROR;

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

	public KeyStoreStateMachine() {
		// addTransition(new MatcherTransition(States.NONE,
		// keyStoreConstructor(),Parameter.This, States.INIT, Type.OnReturn));
		addTransition(new MatcherTransition(States.INIT, loadMethods(), Parameter.This, States.LOADED, Type.OnReturn));

		addTransition(new MatcherTransition(States.INIT, anyMethodOtherThanLoad(), Parameter.This, States.ERROR,
				Type.OnReturn));
		addTransition(new MatcherTransition(States.ERROR, anyMethodOtherThanLoad(), Parameter.This, States.ERROR,
				Type.OnReturn));

	}

	private Set<SootMethod> anyMethodOtherThanLoad() {
		List<SootClass> subclasses = getSubclassesOf("java.security.KeyStore");
		Set<SootMethod> loadMethods = loadMethods();
		Set<SootMethod> out = new HashSet<>();
		for (SootClass c : subclasses) {
			for (SootMethod m : c.getMethods())
				if (m.isPublic() && !loadMethods.contains(m) && !m.isStatic())
					out.add(m);
		}
		return out;
	}

	private Set<SootMethod> loadMethods() {
		return selectMethodByName(getSubclassesOf("java.security.KeyStore"), "load");
	}

	private Set<SootMethod> keyStoreConstructor() {
		List<SootClass> subclasses = getSubclassesOf("java.security.KeyStore");
		Set<SootMethod> out = new HashSet<>();
		for (SootClass c : subclasses) {
			for (SootMethod m : c.getMethods())
				if (m.getName().equals("getInstance") && m.isStatic())
					out.add(m);
		}
		return out;
	}

	@Override
	public Collection<Val> generateSeed(SootMethod m, Unit unit, Collection<SootMethod> calledMethod) {
		if (unit instanceof AssignStmt) {
			AssignStmt stmt = (AssignStmt) unit;
			if(stmt.containsInvokeExpr()){
				if(keyStoreConstructor().contains(stmt.getInvokeExpr().getMethod())){
					Set<Val> out = new HashSet<>();
					out.add(new Val(stmt.getLeftOp(), m));
					return out;
				}
			}
		}
		return Collections.emptySet();
	}

}
