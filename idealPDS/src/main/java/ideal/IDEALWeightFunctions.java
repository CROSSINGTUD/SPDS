package ideal;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public class IDEALWeightFunctions<W extends Weight> implements WeightFunctions<Statement,Val,Statement,W> {

	private WeightFunctions<Statement,Val,Statement,W> delegate;

	public IDEALWeightFunctions(WeightFunctions<Statement,Val,Statement,W>  delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public W push(Node<Statement, Val> curr, Node<Statement, Val> succ, Statement calleeSp) {
		W weight = delegate.push(curr, succ, calleeSp);
		if (!weight.equals(getOne()))	
			System.out.println("Non identity call flow!" + curr + weight);
		return weight;
	}

	@Override
	public W normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
		W weight = delegate.normal(curr, succ);
		if (!weight.equals(getOne()))	
			System.out.println("Non identity normal flow!" + curr + weight);
		return weight;
	}

	@Override
	public W pop(Node<Statement, Val> curr, Statement location) {
		W weight = delegate.pop(curr, location);
		if (!weight.equals(getOne()))	
			System.out.println("Non identity return flow!" + curr + weight);
		return weight;
	}

	@Override
	public W getOne() {
		return delegate.getOne();
	}

	@Override
	public W getZero() {
		return delegate.getZero();
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
		return "[IDEAL-Wrapper Weights] " + delegate.toString();
	}

	
}
