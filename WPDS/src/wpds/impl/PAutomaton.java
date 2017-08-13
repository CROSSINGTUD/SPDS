package wpds.impl;

import pathexpression.LabeledGraph;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public abstract class PAutomaton<N extends Location, D extends State> extends WeightedPAutomaton<N, D, NoWeight<N>>
		implements LabeledGraph<D, N> {

	public PAutomaton(D initialState, D finalState) {
		super(initialState, finalState);
	}

}
