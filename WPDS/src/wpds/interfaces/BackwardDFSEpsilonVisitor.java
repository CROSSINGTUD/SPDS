package wpds.interfaces;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class BackwardDFSEpsilonVisitor<N extends Location,D extends State, W extends Weight> extends BackwardDFSVisitor<N,D,W>{

	public BackwardDFSEpsilonVisitor(WeightedPAutomaton<N, D, W> aut, D startState,
			ReachabilityListener<N, D> listener) {
		super(aut, startState, listener);
	}

	@Override
	protected boolean continueWith(Transition<N, D> t) {
		return t.getLabel().equals(aut.epsilon());
	}
}
