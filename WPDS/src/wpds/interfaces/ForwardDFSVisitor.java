package wpds.interfaces;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class ForwardDFSVisitor<N extends Location,D extends State, W extends Weight<N>> implements WPAUpdateListener<N, D, W>{
	
	private Set<Transition<N, D>> reachable = Sets.newHashSet();
	private ReachabilityListener<N,D> listener;
	private WeightedPAutomaton<N, D, W> aut;
	private D startState;
	
	
	public ForwardDFSVisitor(WeightedPAutomaton<N,D,W> aut, D startState, ReachabilityListener<N,D> listener){
		this.aut = aut;
		this.startState = startState;
		this.listener = listener;

		for(Transition<N, D> t : aut.getTransitionsOutOf(startState)){
			addReachable(t, Sets.<Transition<N, D>>newHashSet());
		}
	}
	

	private void addReachable(Transition<N, D> s, Set<Transition<N, D>> visited) {
		if(!reachable.add(s))
			return;
		if(!visited.add(s))
			return;
		listener.reachable(s);
		Collection<Transition<N, D>> trans = aut.getTransitionsOutOf(s.getTarget());
		for(Transition<N, D> t : trans){
			if(!continueWith(t)){
				continue;
			}
			addReachable(t,visited);
		}
	}



	protected boolean continueWith(Transition<N, D> t) {
		return true;
	}


	@Override
	public void onAddedTransition(Transition<N, D> t) {
		addReachable(t, Sets.<Transition<N, D>>newHashSet());
	}

	@Override
	public void onWeightAdded(Transition<N, D> t, Weight<N> w) {
		
	}
	
}
