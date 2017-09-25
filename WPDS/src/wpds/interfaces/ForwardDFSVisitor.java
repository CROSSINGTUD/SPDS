package wpds.interfaces;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class ForwardDFSVisitor<N extends Location,D extends State, W extends Weight<N>> implements WPAUpdateListener<N, D, W>{
	
	protected WeightedPAutomaton<N, D, W> aut;
	private Multimap<D,ReachabilityListener<N,D>> listeners = HashMultimap.create();
	private Multimap<D, Transition<N,D>> reachable = HashMultimap.create();
	private Multimap<D, Transition<N,D>> outTrans = HashMultimap.create();
	private Set<D> states = Sets.newHashSet();
	
	
	public ForwardDFSVisitor(WeightedPAutomaton<N,D,W> aut){
		this.aut = aut;
	}

	@Override
	public void onWeightAdded(Transition<N, D> t, Weight<N> w) {
		outTrans.put(t.getStart(), t);
		states.add(t.getStart());
		states.add(t.getTarget());
		for(D s : Lists.newLinkedList(states)){
			for(Transition<N,D> o : outTrans.get(s)){
				for(Transition<N,D> reaches :  dfs(o)){
					if(reachable.put(s, reaches)){
						for(ReachabilityListener<N, D> l : listeners.get(s)){
							l.reachable(reaches);
						}
					}
				}
			}
		}
	}

	private Set<Transition<N, D>> dfs(Transition<N, D> t) {
		LinkedList<Transition<N,D>> worklist = Lists.newLinkedList();
		Set<Transition<N,D>> visited = Sets.newHashSet();
		worklist.add(t);
		
		while(!worklist.isEmpty()){
			Transition<N, D> curr = worklist.pop();
			if(!visited.add(curr))
				continue;
			if(!continueWith(curr))
				continue;
			for(Transition<N, D> succ : Lists.newArrayList(outTrans.get(curr.getTarget()))){
				worklist.add(succ);
			}
		}
		return visited;
	}

	public void registerListener(D state, ReachabilityListener<N, D> l) {
		if(listeners.put(state, l)){
			for(Transition<N,D> reaches : Lists.newArrayList(reachable.get(state))){
				l.reachable(reaches);
			}
		}
	}
	
	
	protected boolean continueWith(Transition<N, D> t) {
		return true;
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
