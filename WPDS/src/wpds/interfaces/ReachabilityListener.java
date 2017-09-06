package wpds.interfaces;

import wpds.impl.Transition;

public interface ReachabilityListener<N extends Location, D extends State> {
	void reachable(Transition<N, D> t);
}
