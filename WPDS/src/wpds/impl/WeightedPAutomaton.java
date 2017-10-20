package wpds.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.HashBasedTable;
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
	private Set<ReturnSiteWithWeights> connectedPushes = Sets.newHashSet();
	private Set<ConnectPushListener<N,D,W>> conntectedPushListeners = Sets.newHashSet();
	private Set<UnbalancedPopListener<N,D,W>> unbalancedPopListeners = Sets.newHashSet();
	private Map<Transition<N,D>,W> unbalancedPops = Maps.newHashMap();
	private Map<Transition<N, D>, W> transitionsToFinalWeights = Maps.newHashMap();
	private ForwardDFSVisitor<N, D, W> dfsVisitor;
	private ForwardDFSVisitor<N, D, W> dfsEpsVisitor;
	public int failedAdditions;
	public int failedDirectAdditions;

	public abstract D createState(D d, N loc);

	public abstract boolean isGeneratedState(D d);

	public Collection<Transition<N, D>> getTransitions() {
		return Lists.newArrayList(transitions);
	}
	
	public boolean addTransition(Transition<N, D> trans) {
		boolean addWeightForTransition = addWeightForTransition(trans, getOne());
		if(!addWeightForTransition){
			failedDirectAdditions++;
		}
		return addWeightForTransition;
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
				List<String> labels = Lists.newLinkedList();
				for (Transition<N, D> t : collection) {
					if (t.getTarget().equals(target)) {
						labels.add(t.getString().toString()+ " W: "+ transitionToWeights.get(t));
					}
				}
				if (!labels.isEmpty()) {
					s += "\t\"" + wrapIfInitialOrFinalState(source) + "\"";
					s += " -> \"" + wrapIfInitialOrFinalState(target) + "\"";
					s += "[label=\"" + Joiner.on("\\n").join(labels) + "\"];\n";
				}
			}
		}
		s += "}\n";
		s += "Transitions: " + transitions.size() +"\n";
		for(WeightedPAutomaton<N, D, W> nested : nestedAutomatons){
			s += "NESTED -> \n";
			s += nested.toDotString();
		}
		return s;
	}
	public String toLabelGroupedDotString() {
		HashBasedTable<D, N, Collection<D>> groupedByTargetAndLabel = HashBasedTable.create();
		for(Transition<N, D> t : transitions){
			Collection<D> collection = groupedByTargetAndLabel.get(t.getTarget(), t.getLabel());
			if(collection == null)
				collection = Sets.newHashSet();
			collection.add(t.getStart());
			groupedByTargetAndLabel.put(t.getTarget(), t.getLabel(), collection);
		}
		String s = "digraph {\n";
		for (D target : groupedByTargetAndLabel.rowKeySet()) {
			for (N label : groupedByTargetAndLabel.columnKeySet()){
				Collection<D> source = groupedByTargetAndLabel.get(target, label);
				if(source == null)
					continue;
				s += "\t\"" +Joiner.on("\\n").join(source)+ "\"";
				s += " -> \"" + wrapIfInitialOrFinalState(target) + "\"";
				s += "[label=\"" + label + "\"];\n";
			}
		}
		s += "}\n";
		s += "Transitions: " + transitions.size() +"\n";
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
		if (trans.getStart().equals(trans.getTarget()) && trans.getLabel().equals(epsilon())) {
			failedAdditions++;
			return false;
		}
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
				l.onWeightAdded(trans, newWeight);
			}
			for (WPAStateListener<N, D, W> l : Lists.newArrayList(stateListeners.get(trans.getStart()))) {
				l.onOutTransitionAdded(trans, newWeight);
			}
			for (WPAStateListener<N, D, W> l : Lists.newArrayList(stateListeners.get(trans.getTarget()))) {
				l.onInTransitionAdded(trans, newWeight);
			}
			return true;
		}
		if(!added)
			failedAdditions++;
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
//		ForwardDFSVisitor<N, D, W> dfsVisitor = getStateToDFS().get(state);
		stateToReachabilityListener.put(state,l);
		if (dfsVisitor == null) {
			dfsVisitor = new ForwardDFSVisitor<N, D, W>(this);
//			getStateToDFS().put(state, dfsVisitor);
			this.registerListener(dfsVisitor);
		}
		dfsVisitor.registerListener(state, l);
	}

	protected Map<D, ForwardDFSVisitor<N, D, W>> getStateToDFS() {
		return stateToDFS;
	}

	public void registerDFSEpsilonListener(D state, ReachabilityListener<N, D> l) {
//		ForwardDFSVisitor<N, D, W> dfsVisitor = getStateToEpsilonDFS().get(state);
		stateToEpsilonReachabilityListener.put(state,l);
		if (dfsEpsVisitor == null) {
			dfsEpsVisitor = new ForwardDFSEpsilonVisitor<N, D, W>(this);
//			getStateToEpsilonDFS().put(state, dfsVisitor);
			this.registerListener(dfsEpsVisitor);
		}
		for(WeightedPAutomaton<N, D, W> nested : Lists.newLinkedList(nestedAutomatons)){
			nested.registerDFSEpsilonListener(state, l);
		}
		dfsEpsVisitor.registerListener(state,l);
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

	public void reconnectPush(N callSite, N returnSite,D returnedFact,  W combinedWeight, W returnedWeight) {
		WeightedPAutomaton<N, D, W>.ReturnSiteWithWeights returnSiteWithWeights = new ReturnSiteWithWeights(callSite, returnSite, returnedFact,combinedWeight, returnedWeight);
		if(connectedPushes.add(returnSiteWithWeights )){
			for(ConnectPushListener<N, D, W> l : Lists.newArrayList(conntectedPushListeners)){
				l.connect(returnSiteWithWeights.callSite, returnSiteWithWeights.returnSite, returnSiteWithWeights.returnedFact, returnSiteWithWeights.returnedWeight);
			}
		}
	}
	
	public void registerConnectPushListener(ConnectPushListener<N, D, W> l){
		if(conntectedPushListeners.add(l)){
			for(WeightedPAutomaton<N, D, W>.ReturnSiteWithWeights e : Lists.newArrayList(connectedPushes)){
				l.connect(e.callSite, e.returnSite, e.returnedFact,e.returnedWeight);
			}
		}
	}
	public void registerUnbalancedPopListener(UnbalancedPopListener<N, D, W> l){
		if(unbalancedPopListeners.add(l)){
			for(Entry<Transition<N, D>, W> e : Lists.newArrayList(unbalancedPops.entrySet())){
				Transition<N, D> t = e.getKey();
				l.unbalancedPop(t.getStart(), t.getLabel(), t.getTarget(), e.getValue());
			}
		}
	}

	public void unbalancedPop(D targetState, N popLabel, D target, W weight) {
		Transition<N, D> t = new Transition<N,D>(targetState,popLabel,target);
		W oldVal = unbalancedPops.get(t);
		W newVal = (oldVal == null ? weight : (W) oldVal.combineWith(weight));
		if(!newVal.equals(oldVal)){
			unbalancedPops.put(t, newVal);
			for(UnbalancedPopListener<N, D, W> l : Lists.newArrayList(unbalancedPopListeners)){
				l.unbalancedPop(targetState,popLabel,target, newVal);
			}
		}
	}
	private class ReturnSiteWithWeights{

		private final N returnSite;
		private final W combinedWeight;
		private final W returnedWeight;
		private final D returnedFact;
		private final N callSite;

		public ReturnSiteWithWeights(N callSite, N returnSite, D returnedFact, W combinedWeight, W returnedWeight) {
			this.callSite =  callSite;
			this.returnSite = returnSite;
			this.returnedFact = returnedFact;
			this.combinedWeight = combinedWeight;
			this.returnedWeight = returnedWeight;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
			result = prime * result + ((combinedWeight == null) ? 0 : combinedWeight.hashCode());
			result = prime * result + ((returnSite == null) ? 0 : returnSite.hashCode());
			result = prime * result + ((returnedFact == null) ? 0 : returnedFact.hashCode());
			result = prime * result + ((returnedWeight == null) ? 0 : returnedWeight.hashCode());
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
			ReturnSiteWithWeights other = (ReturnSiteWithWeights) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (callSite == null) {
				if (other.callSite != null)
					return false;
			} else if (!callSite.equals(other.callSite))
				return false;
			if (combinedWeight == null) {
				if (other.combinedWeight != null)
					return false;
			} else if (!combinedWeight.equals(other.combinedWeight))
				return false;
			if (returnSite == null) {
				if (other.returnSite != null)
					return false;
			} else if (!returnSite.equals(other.returnSite))
				return false;
			if (returnedFact == null) {
				if (other.returnedFact != null)
					return false;
			} else if (!returnedFact.equals(other.returnedFact))
				return false;
			if (returnedWeight == null) {
				if (other.returnedWeight != null)
					return false;
			} else if (!returnedWeight.equals(other.returnedWeight))
				return false;
			return true;
		}

		private WeightedPAutomaton getOuterType() {
			return WeightedPAutomaton.this;
		}

		
		
	}
	public void computeValues(Transition<N, D> callTrans, W weight) {
		transitionsToFinalWeights.put(callTrans, weight);
		registerListener(new ValueComputationListener(callTrans));
	}

	public Map<Transition<N,D>, W> getTransitionsToFinalWeights() {
		return transitionsToFinalWeights;
	}
	
	private class ValueComputationListener extends WPAStateListener<N, D, W>{
		private Transition<N,D> trans;

		public ValueComputationListener(Transition<N,D> trans) {
			super(trans.getStart());
			this.trans = trans;
		}

		@Override
		public void onOutTransitionAdded(Transition<N,D> t, W w) {
		}

		@Override
		public void onInTransitionAdded(Transition<N,D> t, W w) {
			W weightAtTarget = transitionsToFinalWeights.get(trans);
			W extendWith = (W) weightAtTarget.extendWith(w);
			W weightAtSource = transitionsToFinalWeights.get(t);
			W newVal = (weightAtSource == null ? extendWith : (W) weightAtSource.combineWith(extendWith));
			if(t.toString().contains("FileMustBeClosedTest.escape return") && t.toString().contains("variable IDEALTestingFramework.mustBeInErrorState i"))
				System.err.println(t + "  " + newVal + " was "+ weightAtSource +"   " + weightAtTarget + w);
			if(!newVal.equals(weightAtSource)){
				transitionsToFinalWeights.put(t, newVal);
				registerListener(new ValueComputationListener(t));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((trans == null) ? 0 : trans.hashCode());
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
			ValueComputationListener other = (ValueComputationListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (trans == null) {
				if (other.trans != null)
					return false;
			} else if (!trans.equals(other.trans))
				return false;
			return true;
		}

		private WeightedPAutomaton getOuterType() {
			return WeightedPAutomaton.this;
		}

	}
	
}
