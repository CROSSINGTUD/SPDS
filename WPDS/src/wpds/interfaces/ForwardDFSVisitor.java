package wpds.interfaces;

import java.util.LinkedList;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class ForwardDFSVisitor<N extends Location,D extends State, W extends Weight> implements WPAUpdateListener<N, D,W>{
	private Multimap<D, ReachabilityListener<N,D>> listeners = HashMultimap.create();
	protected WeightedPAutomaton<N, D, W> aut;
	private Multimap<D, D> adjacent = HashMultimap.create();
	private Multimap<D, D> reaches = HashMultimap.create();
	private Multimap<D, D> inverseReaches = HashMultimap.create();
	private Map<Edge, Index> index = Maps.newHashMap();
	
	public ForwardDFSVisitor(WeightedPAutomaton<N,D,W> aut){
		this.aut = aut;
	}
	public void registerListener(D state, final ReachabilityListener<N, D> l) {
		if(listeners.put(state, l)){
			for(D d : Lists.newArrayList(inverseReaches.get(state))){
				aut.registerListener(new TransitiveClosure(d,state,l));
			}
		}	
	}
	
	private class TransitiveClosure extends WPAStateListener<N,D,W>{

		private D source;
		private ReachabilityListener<N, D> listener;

		public TransitiveClosure(D state, D source, ReachabilityListener<N, D> l) {
			super(state);
			this.source = source;
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
			result = prime * result + ((source == null) ? 0 : source.hashCode());
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
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
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

		D a = t.getStart();
		D b = t.getTarget();
		inverseReaches(a,a);
//		inverseReaches(b,b);
		if(!continueWith(t))
			return;
		insertEdge(a,b);
	}

	private void insertEdge(D a, D b) {
		LinkedList<Edge> worklist = Lists.newLinkedList();
		if(index(a,b).refcount == 0){
			makeClosure(a, b);
			worklist.add(new Edge(a,b));
		}
		makeEdge(a,b);
		index(a, b).refcount += 1;
		
		for(D x : Lists.newArrayList(reaches.get(a))){
			if(index(x,b).refcount == 0){
				makeClosure(x,b);
				worklist.add(new Edge(x,b));
			}
			index(x,b).refcount += 1;
		}
		while(!worklist.isEmpty()){
			Edge e = worklist.poll();
			D x = e.from;
			D y = e.to; 
			for(D z : Lists.newArrayList(adjacent.get(y))){
				if(index(e.from,z).refcount == 0){
					makeClosure(x, z);
					worklist.add(new Edge(x,z));
				}
				index(x,z).refcount += 1;
			}
		}
	}
	private Index index(D from, D to){
		ForwardDFSVisitor<N, D, W>.Edge edge = new Edge(from,to);
		Index i = index.get(edge);
		if(i == null){
			i = new Index();
			index.put(edge, i);
		}
		return i;
	}

	private void makeEdge(D from, D to){
		adjacent.put(from, to);
		inverseReaches(from,to);
	}
	
	private void inverseReaches(D from, D to) {
		if(inverseReaches.put(from, to)){
			for(ReachabilityListener<N, D> l : Lists.newArrayList(listeners.get(from))){
				aut.registerListener(new TransitiveClosure(to, from, l));
			}
		}
		
	}
	private void makeClosure(D from, D to){
		if(reaches.put(to,from)){
			inverseReaches(from,to);
		}
	}
	private class Index{
		int refcount;
	}
	private class Edge{
		final D from;
		final D to;
		private Edge(D from, D to){
			this.from = from;
			this.to = to;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((to == null) ? 0 : to.hashCode());
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
			Edge other = (Edge) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (to == null) {
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			return true;
		}
		private ForwardDFSVisitor getOuterType() {
			return ForwardDFSVisitor.this;
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
