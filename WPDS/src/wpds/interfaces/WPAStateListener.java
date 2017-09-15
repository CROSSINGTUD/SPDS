package wpds.interfaces;

import wpds.impl.Transition;
import wpds.impl.Weight;

public abstract class WPAStateListener<N extends Location, D extends State, W extends Weight<N>> implements WPAUpdateListener<N, D, W>{
	

	private final D state;
	public WPAStateListener(D state) {
		this.state = state;
	}
	@Override
	public void onAddedTransition(Transition<N, D> t) {
		if(t.getStart().equals(state))
			onOutTransitionAdded(t);
		if(t.getTarget().equals(state))
			onInTransitionAdded(t);
	}

	public abstract void onOutTransitionAdded(Transition<N, D> t);
	public abstract void onInTransitionAdded(Transition<N, D> t);
	
	@Override
	public void onWeightAdded(Transition<N, D> t, Weight<N> w) {
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WPAStateListener other = (WPAStateListener) obj;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		return true;
	}
}
