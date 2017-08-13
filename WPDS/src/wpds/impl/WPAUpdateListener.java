package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public interface WPAUpdateListener<N extends Location, D extends State, W extends Weight<N>> {

	void onAddedTransition(Transition<N, D> t);

}
