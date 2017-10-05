package wpds.interfaces;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class ForwardDFSEpsilonVisitor<N extends Location,D extends State, W extends Weight> extends ForwardDFSVisitor<N,D,W>{


	public ForwardDFSEpsilonVisitor(WeightedPAutomaton<N, D, W> aut) {
		super(aut);
	}

	@Override
	protected boolean continueWith(Transition<N, D> t) {
		return t.getLabel() instanceof Empty;
	}
}
