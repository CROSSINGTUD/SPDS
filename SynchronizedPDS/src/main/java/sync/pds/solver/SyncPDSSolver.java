package sync.pds.solver;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import sync.pds.solver.nodes.ExclusionNode;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import sync.pds.solver.nodes.SingleNode;
import sync.pds.weights.SetDomain;
import wpds.impl.NormalRule;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.impl.WeightedPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;

public abstract class SyncPDSSolver<Stmt extends Location, Fact, Field extends Location> {

	public enum PDSSystem {
		FIELDS, CALLS
	}

	private static final boolean DEBUG = true;

	protected final WeightedPushdownSystem<Stmt, INode<Fact>, Weight<Stmt>> callingPDS = new WeightedPushdownSystem<Stmt, INode<Fact>, Weight<Stmt>>() {
		@Override
		public Weight<Stmt> getZero() {
			return SetDomain.zero();
		}

		@Override
		public Weight<Stmt> getOne() {
			return SetDomain.one();
		}
	};
	protected final WeightedPushdownSystem<Field, INode<Node<Stmt,Fact>>, Weight<Field>> fieldPDS = new WeightedPushdownSystem<Field, INode<Node<Stmt,Fact>>, Weight<Field>>() {

		@Override
		public Weight<Field> getZero() {
			return SetDomain.zero();
		}

		@Override
		public Weight<Field> getOne() {
			return SetDomain.one();
		}
	};
	protected final WeightedPAutomaton<Field, INode<Node<Stmt,Fact>>, Weight<Field>> fieldAutomaton = new WeightedPAutomaton<Field, INode<Node<Stmt,Fact>>, Weight<Field>>() {
		@Override
		public INode<Node<Stmt,Fact>> createState(INode<Node<Stmt,Fact>> d, Field loc) {
			if (loc.equals(emptyField()))
				return d;
			return generateFieldState(d, loc);
		}

		@Override
		public Field epsilon() {
			return epsilonField();
		}
	};

	protected final WeightedPAutomaton<Stmt, INode<Fact>,Weight<Stmt>> callAutomaton = new WeightedPAutomaton<Stmt, INode<Fact>,Weight<Stmt>>() {
		@Override
		public INode<Fact> createState(INode<Fact> d, Stmt loc) {
			return generateState(d, loc);
		}

		@Override
		public Stmt epsilon() {
			return epsilonStmt();
		}
	};

	private final Map<WitnessNode<Stmt,Fact,Field>,WitnessNode<Stmt,Fact,Field>> reachedStates = Maps.newHashMap();
	private final Set<Stmt> returnSites = Sets.newHashSet();
	private final Multimap<Node<Stmt,Fact>, Fact> returningFacts = HashMultimap.create();
	private final Set<Node<Stmt, Fact>> callingContextReachable = Sets.newHashSet();
	private final Set<Node<Stmt, Fact>> fieldContextReachable = Sets.newHashSet();
	private final Set<SyncPDSUpdateListener<Stmt, Fact, Field>> updateListeners = Sets.newHashSet();

	private Multimap<WitnessNode<Stmt, Fact, Field>, Transition<Stmt, INode<Fact>>> queuedCallWitness = HashMultimap.create();
	private Multimap<WitnessNode<Stmt, Fact, Field>, Transition<Field, INode<Node<Stmt,Fact>>>> queuedFieldWitness = HashMultimap.create();

	public SyncPDSSolver(){
		callAutomaton.registerListener(new CallAutomatonListener());
		fieldAutomaton.registerListener(new FieldUpdateListener());
		callingPDS.poststar(callAutomaton);
		fieldPDS.poststar(fieldAutomaton);
	}
	
	
	private class CallAutomatonListener implements WPAUpdateListener<Stmt, INode<Fact>,Weight<Stmt>>{

		@Override
		public void onAddedTransition(Transition<Stmt, INode<Fact>> t) {
		}

		@Override
		public void onWeightAdded(Transition<Stmt, INode<Fact>> t, Weight<Stmt> w) {
			if(!(t.getStart() instanceof GeneratedState)){
				setCallingContextReachable(new Node<Stmt,Fact>(t.getString(),t.getStart().fact()), t);
			}
		}
	}

	public void solve(Node<Stmt,Fact> source) {
		solve(source,source);
	}
	
	public void solve(Node<Stmt,Fact> source, Node<Stmt, Fact> curr) {
		Transition<Field, INode<Node<Stmt,Fact>>> fieldTrans = new Transition<Field, INode<Node<Stmt,Fact>>>(asFieldFact(curr), emptyField(), asFieldFact(source));
		fieldAutomaton.addTransition(fieldTrans);
		fieldAutomaton.addWeightForTransition(fieldTrans, new SetDomain<Field,Stmt,Fact>(curr));
		Transition<Stmt, INode<Fact>> callTrans = new Transition<Stmt, INode<Fact>>(wrap(curr.fact()), curr.stmt(), wrap(source.fact()));
		callAutomaton
				.addTransition(callTrans);
		callAutomaton.addWeightForTransition(callTrans, new SetDomain<Stmt,Stmt,Fact>(curr));
		WitnessNode<Stmt, Fact, Field> startNode = new WitnessNode<>(curr.stmt(),curr.fact());
		processNode(startNode);
	}

	private void processNode(WitnessNode<Stmt, Fact,Field> witnessNode) {
		addReachableState(witnessNode);
		Node<Stmt, Fact> curr = witnessNode.asNode();
		Collection<? extends State> successors = computeSuccessor(curr);
//		System.out.println(curr+ " FLows tot \t\t\t "+successors);
		for (State s : successors) {
			if (s instanceof Node) {
				Node<Stmt, Fact> succ = (Node<Stmt, Fact>) s;
				boolean added = false;
				if (succ instanceof PushNode) {
					PushNode<Stmt, Fact, Location> pushNode = (PushNode<Stmt, Fact, Location>) succ;
					PDSSystem system = pushNode.system();
					Location location = pushNode.location();
					added = processPush(curr, location, pushNode, system);
				} else {
					added = processNormal(curr, succ);
				}
				if (added){
					maintainWitness(curr,succ);
					processNode(new WitnessNode<Stmt,Fact,Field>(succ.stmt(),succ.fact()));
				}
			} else if (s instanceof PopNode) {
				PopNode<Fact> popNode = (PopNode<Fact>) s;
				PDSSystem system = popNode.system();
				Fact location = popNode.location();
				processPop(curr, location, system);
			}
		}
	}

	private void maintainWitness(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ) {
		WitnessNode<Stmt, Fact, Field> currWit = new WitnessNode<Stmt,Fact,Field>(curr.stmt(),curr.fact());
		WitnessNode<Stmt, Fact, Field> succWit = new WitnessNode<Stmt,Fact,Field>(succ.stmt(),succ.fact());

		Collection<Transition<Stmt, INode<Fact>>> callWitnesses = queuedCallWitness.get(currWit);
		queuedCallWitness.putAll(succWit, callWitnesses);
		Collection<Transition<Field, INode<Node<Stmt,Fact>>>> fieldWitnesses = queuedFieldWitness.get(currWit);
		queuedFieldWitness.putAll(succWit, fieldWitnesses);
	}

	private void addReachableState(WitnessNode<Stmt,Fact,Field> curr) {
		boolean existed = reachedStates.containsKey(curr);
		if (existed)
			return;
//		System.out.println(this.getClass() + " " + curr);
		reachedStates.put(curr,curr);
		for (SyncPDSUpdateListener<Stmt, Fact, Field> l : Lists.newLinkedList(updateListeners)) {
			l.onReachableNodeAdded(curr);
		}
	}

	private boolean processNormal(Node<Stmt,Fact> curr, Node<Stmt, Fact> succ) {
		boolean added = addNormalFieldFlow(curr, succ);
		added |= addNormalCallFlow(curr, succ);
		return added;
	}

	private boolean addNormalCallFlow(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ) {
		return callingPDS.addRule(
				new NormalRule<Stmt, INode<Fact>,Weight<Stmt>>(wrap(curr.fact()), curr.stmt(), wrap(succ.fact()), succ.stmt(),callingPDS.getOne()));
	}

	public void synchedEmptyStackReachable(final Node<Stmt,Fact> sourceNode, final WitnessListener<Stmt,Fact> listener){
		registerListener(new SyncPDSUpdateListener<Stmt, Fact, Field>() {
			Multimap<Fact, Node<Stmt,Fact>> potentialFieldCandidate = HashMultimap.create();
			Set<Fact> potentialCallCandidate = Sets.newHashSet();
			@Override
			public void onReachableNodeAdded(WitnessNode<Stmt, Fact, Field> reachableNode) {
				if(!reachableNode.asNode().equals(sourceNode))
					return;
				fieldAutomaton.registerListener(new WPAUpdateListener<Field, INode<Node<Stmt,Fact>>, Weight<Field>>() {
					@Override
					public void onAddedTransition(Transition<Field, INode<Node<Stmt, Fact>>> t) {
						if(t.getStart() instanceof GeneratedState || t.getTarget() instanceof GeneratedState)
							return;
						if(!t.getStart().fact().equals(sourceNode))
							return;
						if(!t.getLabel().equals(emptyField()))
							return;
						Node<Stmt, Fact> targetFact = t.getTarget().fact();
						if(!potentialFieldCandidate.put(targetFact.fact(),targetFact))
							return;
						if(potentialCallCandidate.contains(targetFact.fact())){
							listener.witnessFound(targetFact);
						}
					}

					@Override
					public void onWeightAdded(Transition<Field, INode<Node<Stmt, Fact>>> t, Weight<Field> w) {
					}
				});
				callAutomaton.registerListener(new WPAUpdateListener<Stmt, INode<Fact>, Weight<Stmt>>() {
					@Override
					public void onAddedTransition(Transition<Stmt, INode<Fact>> t) {
						if(t.getStart() instanceof GeneratedState || t.getTarget() instanceof GeneratedState)
							return;
						if(!t.getStart().fact().equals(sourceNode.fact()))
							return;
						if(!t.getLabel().equals(sourceNode.stmt()))
							return;
						Fact targetFact = t.getTarget().fact();
						if(!potentialCallCandidate.add(targetFact))
							return;
						if(potentialFieldCandidate.containsKey(targetFact)){
							for(Node<Stmt, Fact> w : potentialFieldCandidate.get(targetFact)){
								listener.witnessFound(w);
							}
						}
					}

					@Override
					public void onWeightAdded(Transition<Stmt, INode<Fact>> t, Weight<Stmt> w) {
						
					}
				});
			}
		});
	}
	
	private boolean addNormalFieldFlow(Node<Stmt,Fact> curr, Node<Stmt, Fact> succ) {
		if (succ instanceof ExclusionNode) {
			ExclusionNode<Stmt, Fact, Field> exNode = (ExclusionNode) succ;
			return fieldPDS.addRule(new NormalRule<Field, INode<Node<Stmt,Fact>>, Weight<Field>>(asFieldFact(curr),
					fieldWildCard(), asFieldFact(succ), exclusionFieldWildCard(exNode.exclusion()), fieldPDS.getOne()));
		}
		return fieldPDS.addRule(new NormalRule<Field, INode<Node<Stmt,Fact>>, Weight<Field>>(asFieldFact(curr),
				fieldWildCard(), asFieldFact(succ), fieldWildCard(), fieldPDS.getOne()));
	}

	public abstract Field exclusionFieldWildCard(Field exclusion);

	public abstract Field fieldWildCard();

	protected INode<Node<Stmt,Fact>> asFieldFact(Node<Stmt, Fact> node) {
		return new SingleNode<Node<Stmt,Fact>>(new Node<Stmt,Fact>(node.stmt(), node.fact()));
	}

	private void processPop(Node<Stmt,Fact> curr, Fact location, PDSSystem system) {
		if (system.equals(PDSSystem.FIELDS)) {
			NodeWithLocation<Stmt, Fact, Field> node = (NodeWithLocation) location;
			fieldPDS.addRule(new PopRule<Field, INode<Node<Stmt,Fact>>, Weight<Field>>(asFieldFact(curr), node.location(),
					asFieldFact(node.fact()), fieldPDS.getOne()));
			addNormalCallFlow(curr, node.fact());
		} else if (system.equals(PDSSystem.CALLS)) {
			//
			callingPDS.addRule(new PopRule<Stmt, INode<Fact>, Weight<Stmt>>(wrap(curr.fact()), curr.stmt(), wrap((Fact) location),callingPDS.getOne()));
			addReturningFact(curr, location);
		}
	}

	public boolean processPush(Node<Stmt,Fact> curr, Location location, Node<Stmt, Fact> succ, PDSSystem system) {
		boolean added = false;
		if (system.equals(PDSSystem.FIELDS)) {
			added |= fieldPDS.addRule(new PushRule<Field, INode<Node<Stmt,Fact>>, Weight<Field>>(asFieldFact(curr),
					fieldWildCard(), asFieldFact(succ),  (Field) location,fieldWildCard(), fieldPDS.getOne()));
			added |= addNormalCallFlow(curr, succ);

		} else if (system.equals(PDSSystem.CALLS)) {
			added |= addNormalFieldFlow(curr, succ);
			added |= callingPDS.addRule(new PushRule<Stmt, INode<Fact>, Weight<Stmt>>(wrap(curr.fact()), curr.stmt(),
					wrap(succ.fact()), succ.stmt(), (Stmt) location,callingPDS.getOne()));

			addReturnSite((Stmt) location);
		}
		return added;
	}


	private class FieldUpdateListener implements WPAUpdateListener<Field, INode<Node<Stmt,Fact>>, Weight<Field>> {

		@Override
		public void onAddedTransition(Transition<Field, INode<Node<Stmt,Fact>>> t) {
			
		}

		@Override
		public void onWeightAdded(Transition<Field, INode<Node<Stmt,Fact>>> t,
				Weight<Field> w) {
			INode<Node<Stmt,Fact>> n = t.getStart();
			if(!(n instanceof GeneratedState)){
				Node<Stmt,Fact> fact = n.fact();
				setFieldContextReachable(new Node<Stmt,Fact>(fact.stmt(), fact.fact()),t);
			}
		}

	}


	private void addReturningFact(Node<Stmt,Fact> curr, Fact fact) {
		if (!returningFacts.put(curr, fact)) {
			return;
		}
		for (Stmt retSite : Lists.newArrayList(returnSites)) {
			callAutomaton.registerListener(new CallUpdateListener(curr, retSite, fact));
		}
	}

	private void addReturnSite(Stmt location) {
		if (!returnSites.add(location)) {
			return;
		}
		for (Entry<Node<Stmt,Fact>, Fact> retSite : Lists.newLinkedList(returningFacts.entries())) {
			callAutomaton.registerListener(new CallUpdateListener(retSite.getKey(), location, retSite.getValue()));
		}
	}

	private class CallUpdateListener implements WPAUpdateListener<Stmt, INode<Fact>, Weight<Stmt>> {
		private Stmt retSite;
		private Fact fact;
		private Node<Stmt,Fact> curr;

		public CallUpdateListener(Node<Stmt,Fact> curr, Stmt retSite, Fact fact) {
			this.curr = curr;
			this.retSite = retSite;
			this.fact = fact;
		}

		@Override
		public void onAddedTransition(Transition<Stmt, INode<Fact>> t) {
			// System.out.println("CALLUPDATE " + t +" " + fact + " "+ retSite);
			if (t.getStart().equals(new SingleNode<Fact>(fact))) {
				if (t.getLabel().equals(retSite)) {
					addNormalFieldFlow(curr, new Node<Stmt, Fact>(retSite, fact));
				}
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((curr == null) ? 0 : curr.hashCode());
			result = prime * result + ((fact == null) ? 0 : fact.hashCode());
			result = prime * result + ((retSite == null) ? 0 : retSite.hashCode());
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
			CallUpdateListener other = (CallUpdateListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (curr == null) {
				if (other.curr != null)
					return false;
			} else if (!curr.equals(other.curr))
				return false;
			if (fact == null) {
				if (other.fact != null)
					return false;
			} else if (!fact.equals(other.fact))
				return false;
			if (retSite == null) {
				if (other.retSite != null)
					return false;
			} else if (!retSite.equals(other.retSite))
				return false;
			return true;
		}

		private SyncPDSSolver getOuterType() {
			return SyncPDSSolver.this;
		}

		@Override
		public void onWeightAdded(Transition<Stmt, INode<Fact>> t, Weight<Stmt> w) {
			
		}

	}

	

	private void setCallingContextReachable(Node<Stmt,Fact> node, Transition<Stmt, INode<Fact>> t) {
		if (!callingContextReachable.add(node))
			return;

		if (fieldContextReachable.contains(node)) {
			processNode(createWitness(node));
		}
	}


	private WitnessNode<Stmt, Fact, Field> createWitness(Node<Stmt, Fact> node) {
		WitnessNode<Stmt, Fact, Field> witnessNode = new WitnessNode<Stmt,Fact,Field>(node.stmt(),node.fact());
		return witnessNode;
	}
	private void setFieldContextReachable(Node<Stmt,Fact> node, Transition<Field, INode<Node<Stmt,Fact>>> t) {
		if (!fieldContextReachable.add(node)) {
			return;
		}

		if (callingContextReachable.contains(node)) {
			processNode(createWitness(node));
		}
	}

	public void registerListener(SyncPDSUpdateListener<Stmt, Fact, Field> listener) {
		if (!updateListeners.add(listener)) {
			return;
		}
		for (WitnessNode<Stmt, Fact, Field> reachableNode : Lists.newArrayList(reachedStates.values())) {
			listener.onReachableNodeAdded(reachableNode);
		}
	}

	private INode<Fact> wrap(Fact variable) {
		return new SingleNode<Fact>(variable);
	}

	Map<Entry<INode<Fact>, Stmt>, INode<Fact>> generatedCallState = Maps.newHashMap();

	protected INode<Fact> generateState(final INode<Fact> d, final Stmt loc) {
		Entry<INode<Fact>, Stmt> e = new AbstractMap.SimpleEntry<>(d, loc);
		if (!generatedCallState.containsKey(e)) {
			generatedCallState.put(e, new GeneratedState<Fact,Stmt>(d,loc));
		}
		return generatedCallState.get(e);
	}

	Map<Entry<INode<Node<Stmt,Fact>>, Field>, INode<Node<Stmt,Fact>>> generatedFieldState = Maps.newHashMap();

	protected INode<Node<Stmt,Fact>> generateFieldState(final INode<Node<Stmt,Fact>> d, final Field loc) {
		Entry<INode<Node<Stmt,Fact>>, Field> e = new AbstractMap.SimpleEntry<>(d, loc);
		if (!generatedFieldState.containsKey(e)) {
			generatedFieldState.put(e, new GeneratedState<Node<Stmt,Fact>,Field>(d,loc));
		}
		return generatedFieldState.get(e);
	}

	public abstract Collection<? extends State> computeSuccessor(Node<Stmt, Fact> node);

	public abstract Field epsilonField();

	public abstract Field emptyField();

	public abstract Stmt epsilonStmt();

	public Set<Node<Stmt, Fact>> getReachedStates() {
		Set<Node<Stmt,Fact>> res = Sets.newHashSet();
		for(WitnessNode<Stmt, Fact, Field> s : reachedStates.keySet())
			res.add(s.asNode());
		return res;
	}

	public void debugOutput() {
		System.out.println(this.getClass());
		System.out.println("All reachable states");
		prettyPrintSet(getReachedStates());

		HashSet<Node<Stmt, Fact>> notFieldReachable = Sets.newHashSet(callingContextReachable);
		notFieldReachable.removeAll(getReachedStates());
		HashSet<Node<Stmt, Fact>> notCallingContextReachable = Sets.newHashSet(fieldContextReachable);
		notCallingContextReachable.removeAll(getReachedStates());
		if (!notFieldReachable.isEmpty()) {
			System.out.println("Calling context matching reachable but not field reachable");
			prettyPrintSet(notFieldReachable);
		}
		if (!notCallingContextReachable.isEmpty()) {
			System.out.println("Field matching reachable");
			prettyPrintSet(notCallingContextReachable);
		}
		System.out.println(fieldPDS);
		System.out.println(fieldAutomaton.toDotString());
		System.out.println(callingPDS);
		System.out.println(callAutomaton.toDotString());
		System.out.println("===== end === "+ this.getClass());
	}

	private void prettyPrintSet(Collection<? extends Object> set) {
		int j = 0;
		for (Object reachableState : set) {
			System.out.print(reachableState);
			System.out.print("\t");
			 if(j++ > 5){
				 System.out.print("\n");
				 j = 0;
			 }
		}
		System.out.println();
	}
}
