package boomerang.weights;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;

public class PathTrackingWeightFunctions implements WeightFunctions<Statement, Val, Statement, DataFlowPathWeight> {

	@Override
	public DataFlowPathWeight push(Node<Statement, Val> curr, Node<Statement, Val> succ, Statement callSite) {
		if(!curr.fact().isStatic()) {
			return new DataFlowPathWeight(callSite);
		}
		return DataFlowPathWeight.one();
	}

	@Override
	public DataFlowPathWeight normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
		if(!curr.fact().equals(succ.fact())) {
			return new DataFlowPathWeight(succ.stmt());
		}
		return DataFlowPathWeight.one();
	}

	@Override
	public DataFlowPathWeight pop(Node<Statement, Val> curr, Statement location) {
		return DataFlowPathWeight.one();
	}

	@Override
	public DataFlowPathWeight getOne() {
		return DataFlowPathWeight.one();
	}

	@Override
	public DataFlowPathWeight getZero() {
		return DataFlowPathWeight.zero();
	}

}
