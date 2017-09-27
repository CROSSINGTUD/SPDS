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
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import pathexpression.Edge;
import pathexpression.IRegEx;
import pathexpression.LabeledGraph;
import pathexpression.PathExpressionComputer;
import pathexpression.RegEx;
import wpds.interfaces.ForwardDFSEpsilonVisitor;
import wpds.interfaces.ForwardDFSVisitor;
import wpds.interfaces.Location;
import wpds.interfaces.ReachabilityListener;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public abstract class WeightedPAutomaton<N extends Location, D extends State, W extends Weight>
		implements LabeledGraph<D, N> {
	private Map<Transition<N, D>, W> transitionToWeights = new HashMap<>();
	// Set Q is implicit
	// Weighted Pushdown Systems and their Application to Interprocedural
	// Dataflow Analysis
	protected Set<Transition<N, D>> transitions = Sets.newHashSet();
	// set F in paper [Reps2003]
	protected Set<D> finalState = Sets.newHashSet();
	// set P in paper [Reps2003]
	protected D initialState;
	protected Set<D> states = Sets.newHashSet();
	private final Multimap<D, Transition<N, D>> transitionsOutOf = HashMultimap.create();
	private final Multimap<D, Transition<N, D>> transitionsInto = HashMultimap.create();
	private Set<WPAUpdateListener<N, D, W>> listeners = Sets.newHashSet();
	private Multimap<D, WPAStateListener<N, D, W>> stateListeners = HashMultimap.create();
	private Map<D, ForwardDFSVisitor<N, D, W>> stateToDFS = Maps.newHashMap();
	private Map<D, ForwardDFSVisitor<N, D, W>> stateToEpsilonDFS = Maps.newHashMap();
	private List<WeightedPAutomaton<N, D, W>> nestedAutomatons = Lists.newArrayList();
	private Map<D, ReachabilityListener<N, D>> stateToEpsilonReachabilityListener = Maps.newHashMap();
	private Map<D, ReachabilityListener<N, D>> stateToReachabilityListener = Maps.newHashMap();

	public abstract D createState(D d, N loc);

	public abstract boolean isGeneratedState(D d);

	public Collection<Transition<N, D>> getTransitions() {
		return Lists.newArrayList(transitions);
	}

	public Collection<Transition<N, D>> getTransitionsOutOf(D state) {
		return Lists.newArrayList(transitionsOutOf.get(state));
	}

	public Collection<Transition<N, D>> getTransitionsInto(D state) {
		return Lists.newArrayList(transitionsInto.get(state));
	}

	public boolean addTransition(Transition<N, D> trans) {
		if (trans.getStart().equals(trans.getTarget()) && trans.getLabel().equals(epsilon())) {
			return false;
		}

		return addWeightForTransition(trans, getOne());
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

		for(WeightedPAutomaton<N, D, W> nested : nestedAutomatons){
			s += "\n";
			s += nested.toString();
		}
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
		for (D source : states) {
			Collection<Transition<N, D>> collection = transitionsOutOf.get(source);
			for (D target : states) {
				List<N> labels = Lists.newLinkedList();
				for (Transition<N, D> t : collection) {
					if (t.getTarget().equals(target)) {
						labels.add(t.getString());
					}
				}
				if (!labels.isEmpty()) {
					s += "\t\"" + wrapIfInitialOrFinalState(source) + "\"";
					s += " -> \"" + wrapIfInitialOrFinalState(target) + "\"";
					s += "[label=\"" + Joiner.on(",").join(labels) + "\"];\n";
				}
			}
		}
		s += "}\n";
		s += "Transitions: " + transitions.size() +"\n";
		// s += "Initial State:" + initialState + "\n";
		// s += "Final States:" + finalState + "\n";
		// s = "digraph {\n";
		//
		// for(Transition<N, D> tran : sequentialTransitions){
		// s += "\t\"" + wrapIfInitialOrFinalState(tran.getStart()) + "\"";
		// s += " -> \"" + wrapIfInitialOrFinalState(tran.getTarget()) + "\"";
		// s += "[label=\"" + tran.getLabel() + "\"];\n";
		// }
		// s += "}\n";
		for(WeightedPAutomaton<N, D, W> nested : nestedAutomatons){
			s += "NESTED -> \n";
			s += nested.toDotString();
		}
		return s;
	}

	public abstract N epsilon();

	public IRegEx<N> extractLanguage(D from) {
		PathExpressionComputer<D, N> expr = new PathExpressionComputer<>(this);
		IRegEx<N> res = null;
		for (D finalState : getFinalState()) {
			IRegEx<N> regEx = expr.getExpressionBetween(from, finalState);
			if (res == null) {
				res = regEx;
			} else {
				res = RegEx.<N> union(res, regEx);
			}
		}
		if (res == null)
			return new RegEx.EmptySet<N>();
		return res;
	}

	public IRegEx<N> extractLanguage(D from, D to) {
		PathExpressionComputer<D, N> expr = new PathExpressionComputer<>(this);
		IRegEx<N> res = expr.getExpressionBetween(from, to);
		if (res == null)
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

	public boolean addWeightForTransition(Transition<N, D> trans, W weight) {
		if (weight == null)
			throw new IllegalArgumentException("Weight must not be null!");
		transitionsOutOf.get(trans.getStart()).add(trans);
		transitionsInto.get(trans.getTarget()).add(trans);
		states.add(trans.getTarget());
		states.add(trans.getStart());
		boolean added = transitions.add(trans);
		W oldWeight = transitionToWeights.get(trans);
		W newWeight = (W) (oldWeight == null ? weight : oldWeight.combineWith(weight));
		if (!newWeight.equals(oldWeight)) {
			transitionToWeights.put(trans, newWeight);
			for (WPAUpdateListener<N, D, W> l : Lists.newArrayList(listeners)) {
				l.onWeightAdded(trans, weight);
			}
			for (WPAStateListener<N, D, W> l : Lists.newArrayList(stateListeners.get(trans.getStart()))) {
				l.onOutTransitionAdded(trans, weight);
			}
			for (WPAStateListener<N, D, W> l : Lists.newArrayList(stateListeners.get(trans.getTarget()))) {
				l.onInTransitionAdded(trans, weight);
			}
		}
		return added;
	}

	public W getWeightFor(Transition<N, D> trans) {
		return transitionToWeights.get(trans);
	}

	public void registerListener(WPAUpdateListener<N, D, W> listener) {
		if (!listeners.add(listener))
			return;
		for (Entry<Transition<N, D>, W> transAndWeight : Lists.newArrayList(transitionToWeights.entrySet())) {
			listener.onWeightAdded(transAndWeight.getKey(), transAndWeight.getValue());
		}
		for(WeightedPAutomaton<N, D, W> nested : Lists.newArrayList(nestedAutomatons)){
			nested.registerListener(listener);
		}
	}

	public void registerListener(WPAStateListener<N, D, W> l) {
		if (!stateListeners.put(l.getState(), l)) {
			return;
		}
		for (Transition<N, D> t : Lists.newArrayList(transitionsOutOf.get(l.getState()))) {
			l.onOutTransitionAdded(t,transitionToWeights.get(t));
		}
		for (Transition<N, D> t : Lists.newArrayList(transitionsInto.get(l.getState()))) {
			l.onInTransitionAdded(t,transitionToWeights.get(t));
		}

		for(WeightedPAutomaton<N, D, W> nested : Lists.newArrayList(nestedAutomatons)){
			nested.registerListener(l);
		}

	}

	public void setInitialState(D state) {
		this.initialState = state;
	}

	public void addFinalState(D state) {
		this.finalState.add(state);
	}

	public void registerDFSListener(D state, ReachabilityListener<N, D> l) {
		ForwardDFSVisitor<N, D, W> dfsVisitor = getStateToDFS().get(state);
		stateToReachabilityListener.put(state,l);
		if (dfsVisitor == null) {
			dfsVisitor = new ForwardDFSVisitor<N, D, W>(this, state);
			getStateToDFS().put(state, dfsVisitor);
			this.registerListener(dfsVisitor);
		}
		dfsVisitor.registerListener(l);
	}

	protected Map<D, ForwardDFSVisitor<N, D, W>> getStateToDFS() {
		return stateToDFS;
	}

	public void registerDFSEpsilonListener(D state, ReachabilityListener<N, D> l) {
		ForwardDFSVisitor<N, D, W> dfsVisitor = getStateToEpsilonDFS().get(state);
		stateToEpsilonReachabilityListener.put(state,l);
		if (dfsVisitor == null) {
			dfsVisitor = new ForwardDFSEpsilonVisitor<N, D, W>(this, state);
			getStateToEpsilonDFS().put(state, dfsVisitor);
			this.registerListener(dfsVisitor);
		}
		for(WeightedPAutomaton<N, D, W> nested : nestedAutomatons){
			nested.registerDFSEpsilonListener(state, l);
		}
		dfsVisitor.registerListener(l);
	}
	
	protected Map<D, ForwardDFSVisitor<N, D, W>> getStateToEpsilonDFS() {
		return stateToEpsilonDFS;
	}

	public abstract W getZero();

	public abstract W getOne();

	public WeightedPAutomaton<N, D, W> createNestedAutomaton() {
		WeightedPAutomaton<N, D, W> nested = new WeightedPAutomaton<N, D, W>() {

			@Override
			public D createState(D d, N loc) {
				return WeightedPAutomaton.this.createState(d, loc);
			}

			@Override
			public N epsilon() {
				return WeightedPAutomaton.this.epsilon();
			}

			@Override
			public W getZero() {
				return WeightedPAutomaton.this.getZero();
			}

			@Override
			public W getOne() {
				return WeightedPAutomaton.this.getOne();
			}

			@Override
			public boolean isGeneratedState(D d) {
				return WeightedPAutomaton.this.isGeneratedState(d);
			}
			
			@Override
			protected Map<D, ForwardDFSVisitor<N, D, W>> getStateToDFS() {
				return WeightedPAutomaton.this.stateToDFS;
			}
			@Override
			protected Map<D, ForwardDFSVisitor<N, D, W>> getStateToEpsilonDFS() {
				return WeightedPAutomaton.this.stateToEpsilonDFS;
			}
			@Override
			public String toString() {
				return "NESTED: \n" + super.toString();
			}
		};
		nestedAutomatons.add(nested);
		
		for(WPAStateListener<N, D, W> e : Lists.newArrayList(stateListeners.values())){
			nested.registerListener(e);
		}
		for(WPAUpdateListener<N, D, W> e : Lists.newArrayList(listeners)){
			nested.registerListener(e);
		}
		
		return nested;
	}
}
