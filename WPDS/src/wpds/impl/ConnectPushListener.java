package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public interface ConnectPushListener<N extends Location, D extends State, W extends Weight> {

	void connect(N callSite, N returnSite,D returnedFact,  W returnedWeight);

}
