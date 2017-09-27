package typestate;

import java.util.Collection;
import java.util.Set;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Unit;
import sync.pds.solver.nodes.Node;
import typestate.finiteautomata.State;
import typestate.finiteautomata.Transition;

public interface TypestateChangeFunction {
	Set<? extends Transition<State>> getReturnTransitionsFor(Node<Statement,Val> curr, Statement returnStmt);
	Set<? extends Transition<State>> getCallTransitionsFor(Node<Statement,Val> curr, Node<Statement,Val> succ, Statement calleeSp);

	Collection<Val> generateSeed(SootMethod method, Unit stmt, Collection<SootMethod> optional);
}
