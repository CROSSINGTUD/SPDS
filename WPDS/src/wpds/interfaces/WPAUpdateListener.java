package wpds.interfaces;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public interface WPAUpdateListener<N extends Location, D extends State, W extends Weight> {
	void onWeightAdded(Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> aut);
}
