package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;

public abstract class StackListener<N extends Location, D extends State, W extends Weight> extends WPAStateListener<N,D,W>{
	/**
	 * 
	 */
	private final WeightedPAutomaton<N, D, W> aut;
	private N source;
	public StackListener(WeightedPAutomaton<N, D, W> weightedPAutomaton, D state, N source) {
		super(state);
		this.aut = weightedPAutomaton;
		this.source = source;
	}
	@Override
	public void onOutTransitionAdded(Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> weightedPAutomaton) {
		if(this.aut.isGeneratedState(t.getTarget()) && t.getLabel().equals(source)) {
			aut.registerListener(new SubStackListener(t.getTarget(),source, this) {
				@Override
				public void stackElement(N child, N parent) {
					StackListener.this.stackElement(child, parent);
				}
			});
		}
		if(this.aut.initialState.equals(t.getTarget()) && t.getLabel().equals(source)) {
			anyContext(source);
		}
	}
	@Override
	public void onInTransitionAdded(Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> weightedPAutomaton) {
	}
	public abstract void stackElement(N child, N parent);
	public abstract void anyContext(N end);
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		StackListener<N, D, W> other = (StackListener<N, D, W>) obj;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}
	abstract class SubStackListener extends WPAStateListener<N, D, W>{
		private N source;
		private StackListener parent;
		public SubStackListener(D state, N source, StackListener parent) {
			super(state);
			this.source = source;
			this.parent = parent;
		}
		@Override
		public void onOutTransitionAdded(Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> weightedPAutomaton) {
			stackElement(source, t.getLabel());
			if(aut.isGeneratedState(t.getTarget())) {
				aut.registerListener(new SubStackListener(t.getTarget(),t.getLabel(),parent) {
					@Override
					public void stackElement(N child, N parent) {
						SubStackListener.this.stackElement(child, parent);
					}
				});
			}
		}

		@Override
		public void onInTransitionAdded(Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> weightedPAutomaton) {
		}
		public abstract void stackElement(N child, N parent);
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			result = prime * result + ((parent == null) ? 0 : parent.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			SubStackListener other = (SubStackListener) obj;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (parent == null) {
				if (other.parent != null)
					return false;
			} else if (!parent.equals(other.parent))
				return false;
			return true;
		}
	} 
}