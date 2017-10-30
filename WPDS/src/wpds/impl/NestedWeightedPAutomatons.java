package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public interface NestedWeightedPAutomatons<N extends Location, D extends State, W extends Weight> {

	void putSummaryAutomaton(D target, WeightedPAutomaton<N, D, W> aut);

	WeightedPAutomaton<N, D, W> getSummaryAutomaton(D target);

}
