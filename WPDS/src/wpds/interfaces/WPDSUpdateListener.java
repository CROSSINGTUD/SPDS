package wpds.interfaces;

import wpds.impl.Rule;
import wpds.impl.Weight;

public interface WPDSUpdateListener<N extends Location, D extends State, W extends Weight<N>> {

	public void onRuleAdded(Rule<N, D, W> rule);

}
