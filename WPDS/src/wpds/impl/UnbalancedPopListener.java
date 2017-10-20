package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public interface UnbalancedPopListener<N extends Location, D extends State, W extends Weight> {

	void unbalancedPop(D returningFact, N popLabel,D targetFact, W weight);

}
