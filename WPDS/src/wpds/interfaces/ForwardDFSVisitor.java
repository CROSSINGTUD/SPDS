package wpds.interfaces;

import java.util.Collection;
import java.util.LinkedList;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class ForwardDFSVisitor<N extends Location,D extends State, W extends Weight> implements WPAUpdateListener<N, D,W>{
	private Multimap<D, ReachabilityListener<N,D>> listeners = HashMultimap.create();
	protected WeightedPAutomaton<N, D, W> aut;
	private Multimap<D, D> transitiveClosure = HashMultimap.create();
	private Multimap<D, D> inverseTransitiveClosure = HashMultimap.create();
	
	public ForwardDFSVisitor(WeightedPAutomaton<N,D,W> aut){
		this.aut = aut;
	}
	public void registerListener(D state, final ReachabilityListener<N, D> l) {
		if(listeners.put(state, l)){
			for(D d : Lists.newArrayList(transitiveClosure.get(state))){
				aut.registerListener(new TransitiveClosure(d,state,l));
			}
		}	
	}
	
	private class TransitiveClosure extends WPAStateListener<N,D,W>{

//		private D source;
		private ReachabilityListener<N, D> listener;

		public TransitiveClosure(D state, D source, ReachabilityListener<N, D> l) {
			super(state);
//			this.source = source;
			this.listener = l;
		}
		@Override
		public void onOutTransitionAdded(Transition<N, D> t, W w) {
			listener.reachable(t);
		}

		@Override
		public void onInTransitionAdded(Transition<N, D> t, W w) {
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
//			result = prime * result + ((source == null) ? 0 : source.hashCode());
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
			TransitiveClosure other = (TransitiveClosure) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
//			if (source == null) {
//				if (other.source != null)
//					return false;
//			} else if (!source.equals(other.source))
//				return false;
			if (listener == null) {
				if (other.listener != null)
					return false;
			} else if (!listener.equals(other.listener))
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
	public void onWeightAdded(Transition<N, D> t, W w) {

		D i = t.getStart();
		D j = t.getTarget();
		addTransitiveClosure(i, i);
		addTransitiveClosure(j, j);
		if(!continueWith(t))
			return;
		insertStar(i,j);
	}


	private void addTransitiveClosure(D i, D j) {
		if(transitiveClosure.put(i, j)){
			inverseTransitiveClosure.put(j, i);
			for(final ReachabilityListener<N, D> listener : Lists.newArrayList(listeners.get(i))){
				aut.registerListener(new TransitiveClosure(j, i, listener));
			}
		}
	}
	private void insertStar(D i, D j) {
		Collection<D> col = Sets.newHashSet(inverseTransitiveClosure.get(i));
		if(col.contains(j))
			return;
		Collection<D> intoJ = inverseTransitiveClosure.get(j);
		for(D k : col){
			if(!intoJ.contains(k))
				adaptStar(j,k);
		}
	}
	
	
	private void adaptStar(D j, D k) {
		LinkedList<D> redNodes = Lists.newLinkedList();
		redNodes.add(j);
		while(!redNodes.isEmpty()){
			D l = redNodes.poll();
			addTransitiveClosure(k, l);
			for(Transition<N,D> t : aut.getTransitionsOutOf(l)){
				Collection<D> outOfK = transitiveClosure.get(k);
				D m = t.getTarget();
				if(!outOfK.contains(m)){
					if(!continueWith(t))
						continue;
					redNodes.add(m);
				}
			}
		}
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((aut == null) ? 0 : aut.hashCode());
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
		return true;
	}


	
	
	
}
