package wpds.interfaces;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class ForwardDFSVisitor<N extends Location,D extends State, W extends Weight<N>> implements WPAUpdateListener<N, D, W>{
	
	private Set<D> reachableStates = Sets.newHashSet();
	private ReachabilityListener<N,D> listener;
	private WeightedPAutomaton<N, D, W> aut;
	private Set<Transition<N,D>> visited =  Sets.newHashSet();
	
	
	public ForwardDFSVisitor(WeightedPAutomaton<N,D,W> aut, D startState, ReachabilityListener<N,D> listener){
		this.aut = aut;
		this.listener = listener;
		this.reachableStates.add(startState);
		for(Transition<N, D> t : aut.getTransitionsOutOf(startState)){
			addReachable(t);
		}
	}
	

	private void addReachable(Transition<N, D> s) {
		if(!visited.add(s))
			return;
		listener.reachable(s);
		Collection<Transition<N, D>> trans = aut.getTransitionsOutOf(s.getTarget());
		reachableStates.add(s.getTarget());
		for(Transition<N, D> t : trans){
			if(!continueWith(t)){
				continue;
			}
			addReachable(t);
		}
	}



	protected boolean continueWith(Transition<N, D> t) {
		return true;
	}


	@Override
	public void onAddedTransition(Transition<N, D> t) {
		if(reachableStates.contains(t.getStart()))
			addReachable(t);
	}

	@Override
	public void onWeightAdded(Transition<N, D> t, Weight<N> w) {
		
	}
	
}
