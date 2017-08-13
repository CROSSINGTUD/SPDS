package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public interface WPDSUpdateListener<N extends Location, D extends State, W extends Weight<N>> {

	public void onRuleAdded(Rule<N, D, W> rule);

}
