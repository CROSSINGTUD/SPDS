package wpds.interfaces;

import wpds.impl.Transition;
import wpds.impl.Weight;

public interface WPAUpdateListener<N extends Location, D extends State, W extends Weight> {
	void onWeightAdded(Transition<N, D> t, Weight w);
}
