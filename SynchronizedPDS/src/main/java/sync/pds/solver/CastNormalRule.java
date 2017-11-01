package sync.pds.solver;

import wpds.impl.NormalRule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public class CastNormalRule<N extends Location, D extends State, W extends Weight> extends NormalRule<N,D,W>{

	public CastNormalRule(D s1, N l1, D s2, N l2, W w) {
		super(s1, l1, s2, l2, w);
	}

	@Override
	public boolean canBeApplied(Transition<N, D> t, W weight) {
		return super.canBeApplied(t, weight);
	}
}
