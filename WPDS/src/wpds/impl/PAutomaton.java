package wpds.impl;

import pathexpression.LabeledGraph;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public abstract class PAutomaton<N extends Location, D extends State> extends WeightedPAutomaton<N, D, NoWeight<N>>
		implements LabeledGraph<D, N> {
	
	@Override
	public NoWeight<N> getOne() {
		return NoWeight.NO_WEIGHT_ONE;
	}

	@Override
	public NoWeight<N> getZero() {
		return NoWeight.NO_WEIGHT_ZERO;
	}
}
