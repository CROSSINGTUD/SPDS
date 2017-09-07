package wpds.interfaces;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class ForwardDFSVisitor<N extends Location,D extends State, W extends Weight<N>> implements WPAUpdateListener<N, D, W>{
	
	private Set<D> reachable = Sets.newHashSet();
	private ReachabilityListener<N,D> listener;
	private WeightedPAutomaton<N, D, W> aut;
	
	
	public ForwardDFSVisitor(WeightedPAutomaton<N,D,W> aut, D startState, ReachabilityListener<N,D> listener){
		this.aut = aut;
		this.listener = listener;
		addReachable(startState, Sets.<D>newHashSet());
	}
	

	private void addReachable(D s, Set<D> visited) {
		if(!reachable.add(s))
			return;
		if(!visited.add(s))
			return;
		Collection<Transition<N, D>> trans = aut.getTransitionsOutOf(s);
		for(Transition<N, D> t : trans){
			if(!continueWith(t)){
				continue;
			}
			listener.reachable(t);
			addReachable(t.getTarget(),visited);
		}
	}



	protected boolean continueWith(Transition<N, D> t) {
		return true;
	}


	@Override
	public void onAddedTransition(Transition<N, D> t) {
		if(reachable.contains(t.getStart()))
			addReachable(t.getTarget(), Sets.<D>newHashSet());
	}

	@Override
	public void onWeightAdded(Transition<N, D> t, Weight<N> w) {
		
	}
	
}
