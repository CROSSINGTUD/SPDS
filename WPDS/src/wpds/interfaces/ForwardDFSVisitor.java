package wpds.interfaces;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public abstract class ForwardDFSVisitor<N extends Location,D extends State, W extends Weight<N>> implements WPAUpdateListener<N, D, W>{
	
	private D startState;
	private Set<D> reachable = Sets.newHashSet();
	private Set<ReachabilityListener<D>> listeners = Sets.newHashSet();
	private WeightedPAutomaton<N, D, W> aut;
	
	
	public ForwardDFSVisitor(WeightedPAutomaton<N,D,W> aut, D startState){
		this.aut = aut;
		this.startState = startState;
		addReachable(startState, Sets.<D>newHashSet());
	}
	

	private void addReachable(D s, Set<D> visited) {
		if(!reachable.add(s))
			return;
		if(!visited.add(s))
			return;
		for(ReachabilityListener<D> l : listeners){
			l.reachable(s);
		}
		Collection<Transition<N, D>> trans = aut.getTransitionsOutOf(s);
		for(Transition<N, D> t : trans ){
			addReachable(t.getTarget(),visited);
		}
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
