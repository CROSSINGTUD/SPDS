package typestate;

import java.util.Set;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import typestate.finiteautomata.State;
import typestate.finiteautomata.Transition;

public class TypestateEdgeFunctions implements WeightFunctions<Statement,Val,Statement,TransitionFunction<State>> {

	private TypestateChangeFunction func;

	public TypestateEdgeFunctions(TypestateChangeFunction func) {
		this.func = func;
	}
	
	@Override
	public TransitionFunction<State> push(Node<Statement, Val> curr, Node<Statement, Val> succ, Statement calleeSp) {
		Set<? extends Transition<State>> trans = func.getCallTransitionsFor(curr, succ, calleeSp);
		if (trans.isEmpty())	
			return TransitionFunction.one();
		return new TransitionFunction<State>(trans);
	}

	@Override
	public TransitionFunction<State> normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
		return TransitionFunction.<State>one();
	}

	@Override
	public TransitionFunction<State> pop(Node<Statement, Val> curr, Statement location) {
		Set<? extends Transition<State>> trans = func.getReturnTransitionsFor(curr, location);
		if (trans.isEmpty())
			return TransitionFunction.<State>one();
		return new TransitionFunction<State>(trans);
	}

	@Override
	public TransitionFunction<State> getOne() {
		return TransitionFunction.<State>one();
	}

	@Override
	public TransitionFunction<State> getZero() {
		return TransitionFunction.<State>zero();
	}


//	@Override
//	public EdgeFunction<TypestateDomainValue<State>> getCallToReturnEdgeFunction(AccessGraph d1, Unit callSite, AccessGraph d2,
//			Unit returnSite, AccessGraph d3) {
//		Set<? extends Transition<State>> trans = func.getCallToReturnTransitionsFor(d1, callSite, d2, returnSite, d3);
//		if (trans.isEmpty())
//			return EdgeIdentity.v();
//		return new TransitionFunction<State>(trans);
//	}

	
	@Override
	public String toString() {
		return func.toString();
	}

	
}
