package wpds.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import pathexpression.Edge;
import pathexpression.IRegEx;
import pathexpression.LabeledGraph;
import pathexpression.PathExpressionComputer;
import pathexpression.RegEx;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;

public abstract class WeightedPAutomaton<N extends Location, D extends State, W extends Weight<N>>
		implements LabeledGraph<D, N> {
	private Map<Transition<N, D>, W> transitionToWeights = new HashMap<>();
	// Set Q is implicit
	// Weighted Pushdown Systems and their Application to Interprocedural
	// Dataflow Analysis
	protected Set<Transition<N, D>> transitions = Sets.newHashSet();
	protected List<Transition<N, D>> sequentialTransitions = Lists.newArrayList();
	// set F in paper [Reps2003]
	protected Set<D> finalState = Sets.newHashSet();
	// set P in paper [Reps2003]
	protected D initialState;
	protected Set<D> states = Sets.newHashSet();
	private final Multimap<D, Transition<N, D>> transitionsOutOf = HashMultimap.create();
	private final Multimap<D, Transition<N, D>> transitionsInto = HashMultimap.create();
	private Set<WPAUpdateListener<N, D, W>> listeners = Sets.newHashSet();

	public WeightedPAutomaton() {
	}

	public abstract D createState(D d, N loc);

	public Set<Transition<N, D>> getTransitions() {
		return Sets.newHashSet(transitions);
	}

	public Collection<Transition<N, D>> getTransitionsOutOf(D state) {
		return Sets.newHashSet(transitionsOutOf.get(state));
	}

	public Collection<Transition<N, D>> getTransitionsInto(D state) {
		return new HashSet<>(transitionsInto.get(state));
	}

	public boolean addTransition(Transition<N, D> trans) {
		if(trans.getStart().equals(trans.getTarget()) && trans.getLabel().equals(epsilon())){
			return false;
		}
		transitionsOutOf.get(trans.getStart()).add(trans);
		transitionsInto.get(trans.getTarget()).add(trans);
		states.add(trans.getTarget());
		states.add(trans.getStart());
		boolean added = transitions.add(trans);
		sequentialTransitions.add(trans);
		if(added){
			for(WPAUpdateListener<N, D, W> l : Lists.newLinkedList(listeners)){
				l.onAddedTransition(trans);
			}
		}
		return added;
	}

	public D getInitialState() {
		return initialState;
	}

	public Set<D> getFinalState() {
		return finalState;
	}

	public String toString() {
		String s = "PAutomaton\n";
		s += "\tInitialStates:" + initialState + "\n";
		s += "\tFinalStates:" + finalState + "\n";
		s += "\tWeightToTransitions:\n\t\t";
		s += Joiner.on("\n\t\t").join(transitionToWeights.entrySet());
		
		
		return s;
	}


	private String wrapIfInitialOrFinalState(D s) {
		return s.equals(initialState) ? "ENTRY: " + wrapFinalState(s) : wrapFinalState(s);
	}

	private String wrapFinalState(D s) {
		return finalState.contains(s) ? "TO: " + s + "" : s.toString();
	}

	public String toDotString() {
		String s = "digraph {\n";
		for(D source : states){
			Collection<Transition<N, D>> collection = transitionsOutOf.get(source);
			for(D target : states){
				List<N> labels = Lists.newLinkedList();
				for(Transition<N, D> t : collection){
					if(t.getTarget().equals(target)){
						labels.add(t.getString());
					}
				}
				if(!labels.isEmpty()){
					s += "\t\"" + wrapIfInitialOrFinalState(source) + "\"";
					s += " -> \"" + wrapIfInitialOrFinalState(target) + "\"";
					s += "[label=\"" + Joiner.on(",").join(labels) + "\"];\n";
				}
			}
		}
		s += "}\n";
//		s += "Initial State:" + initialState + "\n";
//		s += "Final States:" + finalState + "\n";
//		s = "digraph {\n";
//
//		for(Transition<N, D> tran : sequentialTransitions){
//			s += "\t\"" + wrapIfInitialOrFinalState(tran.getStart()) + "\"";
//			s += " -> \"" + wrapIfInitialOrFinalState(tran.getTarget()) + "\"";
//			s += "[label=\"" + tran.getLabel() + "\"];\n";
//		}
//		s += "}\n";
		return s;
	}
	public abstract N epsilon();

	public IRegEx<N> extractLanguage(D from) {
		PathExpressionComputer<D, N> expr = new PathExpressionComputer<>(this);
		IRegEx<N> res = null;
		for(D finalState : getFinalState()){
			IRegEx<N> regEx = expr.getExpressionBetween(from, finalState);
			if(res == null){
				res = regEx;
			} else {
				res = RegEx.<N>union(res, regEx);
			}
		}
		if(res == null)
			return new RegEx.EmptySet<N>();
		return res;
	}

	public IRegEx<N> extractLanguage(D from, D to) {
		PathExpressionComputer<D, N> expr = new PathExpressionComputer<>(this);
		IRegEx<N> res = expr.getExpressionBetween(from, to);
		if(res == null)
			return new RegEx.EmptySet<N>();
		return res;
	}
	public Set<D> getStates() {
		return states;
	}

	public Set<Edge<D, N>> getEdges() {
		Set<Edge<D, N>> trans = Sets.newHashSet();
		for (Edge<D, N> tran : transitions)
			trans.add(tran);
		return trans;
	};

	public Set<D> getNodes() {
		return getStates();
	};

	public void addWeightForTransition(Transition<N, D> trans, W weight) {
		W w = transitionToWeights.get(trans);
		if(w == null || !w.equals(weight)){
			transitionToWeights.put(trans, weight);
			for(WPAUpdateListener<N, D, W> l : Lists.newLinkedList(listeners))
				l.onWeightAdded(trans, weight);
		}
	}

	public W getWeightFor(Transition<N, D> trans) {
		return transitionToWeights.get(trans);
	}

	public void registerListener(WPAUpdateListener<N,D,W> listener){
		if(!listeners.add(listener))
			return;
		for(Transition<N, D > t : getTransitions()){
			listener.onAddedTransition(t);
		}
		for(Entry<Transition<N, D>, W> transAndWeight : transitionToWeights.entrySet()){
			listener.onWeightAdded(transAndWeight.getKey(), transAndWeight.getValue());
		}
	}

	public void setInitialState(D state) {
		this.initialState = state;
	}
	
	public void addFinalState(D state) {
		this.finalState.add(state);
	}

	public Set<Transition<N,D>> dfs(D state) {
		Set<Transition<N,D>> visited = Sets.newHashSet();
		LinkedList<Transition<N,D>> worklist = Lists.newLinkedList(getTransitionsInto(state));
		while(!worklist.isEmpty()){
			Transition<N, D> poll = worklist.poll();
			if(!visited.add(poll)){
				continue;
			}
			worklist.addAll(getTransitionsInto(poll.getTarget()));
		}
		return visited;
	}
}
