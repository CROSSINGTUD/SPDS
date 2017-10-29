package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public interface NestedAutomatonListener<N extends Location, D extends State, W extends Weight> {
	void nestedAutomaton(WeightedPAutomaton<N, D, W> parent, WeightedPAutomaton<N, D, W> child);
}
