package typestate;

import java.util.Set;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import typestate.finiteautomata.Transition;

public class TypestateEdgeFunctions implements WeightFunctions<Statement,Val,Statement,TransitionFunction> {

	private TypestateChangeFunction func;

	public TypestateEdgeFunctions(TypestateChangeFunction func) {
		this.func = func;
	}
	
	@Override
	public TransitionFunction push(Node<Statement, Val> curr, Node<Statement, Val> succ, Statement calleeSp) {
		Set<? extends Transition> trans = func.getCallTransitionsFor(curr, succ, calleeSp);
		if (trans.isEmpty())	
			return TransitionFunction.one();
		return new TransitionFunction(trans);
	}

	@Override
	public TransitionFunction normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
		return TransitionFunction.one();
	}

	@Override
	public TransitionFunction pop(Node<Statement, Val> curr, Statement location) {
		Set<? extends Transition> trans = func.getReturnTransitionsFor(curr, location);
		if (trans.isEmpty())
			return TransitionFunction.one();
		return new TransitionFunction(trans);
	}

	@Override
	public TransitionFunction getOne() {
		return TransitionFunction.one();
	}

	@Override
	public TransitionFunction getZero() {
		return TransitionFunction.zero();
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
