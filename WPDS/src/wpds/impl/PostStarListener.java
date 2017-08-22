package wpds.impl;

import java.util.List;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public interface PostStarListener<N extends Location, D extends State, W extends Weight<N>> {

	void update(Rule<N, D, W> triggeringRule, Transition<N, D> trans, List<Transition<N, D>> previous);

}
