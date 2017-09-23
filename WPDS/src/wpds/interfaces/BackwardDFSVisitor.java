package wpds.interfaces;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class BackwardDFSVisitor<N extends Location,D extends State, W extends Weight<N>> implements WPAUpdateListener<N, D, W>{
	
	private Set<D> reachableStates = Sets.newHashSet();
	private ReachabilityListener<N,D> listener;
	protected WeightedPAutomaton<N, D, W> aut;
	private Set<Transition<N,D>> visited =  Sets.newHashSet();
	
	
	public BackwardDFSVisitor(WeightedPAutomaton<N,D,W> aut, D startState, ReachabilityListener<N,D> listener){
		this.aut = aut;
		this.listener = listener;
		this.reachableStates.add(startState);
		for(Transition<N, D> t : aut.getTransitionsInto(startState)){
			addReachable(t);
		}
	}
	

	private void addReachable(Transition<N, D> s) {
		if(!visited.add(s))
			return;

		listener.reachable(s);
		if(!continueWith(s)){
			return;
		}
		Collection<Transition<N, D>> trans = aut.getTransitionsInto(s.getStart());
		reachableStates.add(s.getStart());
		for(Transition<N, D> t : trans){
			addReachable(t);
		}
	}



	protected boolean continueWith(Transition<N, D> t) {
		return true;
	}


	@Override
	public void onWeightAdded(Transition<N, D> t, Weight<N> w) {
		if(reachableStates.contains(t.getTarget()))
			addReachable(t);
	}
	
}
