package wpds.impl;

import pathexpression.LabeledGraph;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public abstract class PAutomaton<N extends Location, D extends State> extends WeightedPAutomaton<N, D, NoWeight>
		implements LabeledGraph<D, N> {
	
	public PAutomaton(D initialState) {
		super(initialState);
	}

	@Override
	public NoWeight getOne() {
		return NoWeight.NO_WEIGHT_ONE;
	}

	@Override
	public NoWeight getZero() {
		return NoWeight.NO_WEIGHT_ZERO;
	}
}
