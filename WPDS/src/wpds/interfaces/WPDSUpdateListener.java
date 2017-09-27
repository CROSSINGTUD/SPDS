package wpds.interfaces;

import wpds.impl.Rule;
import wpds.impl.Weight;

public interface WPDSUpdateListener<N extends Location, D extends State, W extends Weight> {

	public void onRuleAdded(Rule<N, D, W> rule);

}
