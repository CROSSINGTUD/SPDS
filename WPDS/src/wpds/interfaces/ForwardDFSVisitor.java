package wpds.interfaces;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class ForwardDFSVisitor<N extends Location,D extends State, W extends Weight<N>> extends WPAStateListener<N, D, W>{
	
	private Set<D> reachableStates = Sets.newHashSet();
	private ReachabilityListener<N,D> listener;
	protected WeightedPAutomaton<N, D, W> aut;
	private Set<Transition<N,D>> visited =  Sets.newHashSet();
	
	
	public ForwardDFSVisitor(WeightedPAutomaton<N,D,W> aut, D startState, ReachabilityListener<N,D> listener){
		super(startState);
		this.aut = aut;
		this.listener = listener;
		this.reachableStates.add(startState);
	}
	

	private void addReachable(Transition<N, D> s) {
		if(!visited.add(s))
			return;

		listener.reachable(s);
		if(!continueWith(s)){
			return;
		}
		
		aut.registerListener(new TransitiveListener(s.getTarget(),getState()));
	}
	
	private class TransitiveListener extends WPAStateListener<N, D, W>{

		private D startState;

		public TransitiveListener(D state, D startState) {
			super(state);
			this.startState = startState;
		}

		@Override
		public void onOutTransitionAdded(Transition<N, D> t) {
			addReachable(t);	
		}

		@Override
		public void onInTransitionAdded(Transition<N, D> t) {
			
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((startState == null) ? 0 : startState.hashCode());
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
			TransitiveListener other = (TransitiveListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (startState == null) {
				if (other.startState != null)
					return false;
			} else if (!startState.equals(other.startState))
				return false;
			return true;
		}

		private ForwardDFSVisitor getOuterType() {
			return ForwardDFSVisitor.this;
		}
		
		
	}



	protected boolean continueWith(Transition<N, D> t) {
		return true;
	}



	@Override
	public void onOutTransitionAdded(Transition<N, D> t) {
		addReachable(t);
	}


	@Override
	public void onInTransitionAdded(Transition<N, D> t) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((aut == null) ? 0 : aut.hashCode());
		result = prime * result + ((listener == null) ? 0 : listener.hashCode());
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
		ForwardDFSVisitor other = (ForwardDFSVisitor) obj;
		if (aut == null) {
			if (other.aut != null)
				return false;
		} else if (!aut.equals(other.aut))
			return false;
		if (listener == null) {
			if (other.listener != null)
				return false;
		} else if (!listener.equals(other.listener))
			return false;
		return true;
	}
	
	
}
