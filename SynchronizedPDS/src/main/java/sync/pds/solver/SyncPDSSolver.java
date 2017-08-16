package sync.pds.solver;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import sync.pds.solver.nodes.ExclusionNode;
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
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.impl.WeightedPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;
import wpds.interfaces.WPDSUpdateListener;

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
	protected final WeightedPushdownSystem<Field, INode<StmtWithFact>, Weight<Field>> fieldPDS = new WeightedPushdownSystem<Field, INode<StmtWithFact>, Weight<Field>>() {

		@Override
		public Weight<Field> getZero() {
			return SetDomain.zero();
		}

		@Override
		public Weight<Field> getOne() {
			return SetDomain.one();
		}
	};
	protected final WeightedPAutomaton<Field, INode<StmtWithFact>, Weight<Field>> fieldAutomaton = new WeightedPAutomaton<Field, INode<StmtWithFact>, Weight<Field>>() {
		@Override
		public INode<StmtWithFact> createState(INode<StmtWithFact> d, Field loc) {
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

	private final LinkedList<QueuedNode> worklist = Lists.newLinkedList();
	private final Set<QueuedNode> reachedStates = Sets.newHashSet();
	private final Set<Stmt> returnSites = Sets.newHashSet();
	private final Multimap<QueuedNode, Fact> returningFacts = HashMultimap.create();
	private final Set<Node<Stmt, Fact>> callingContextReachable = Sets.newHashSet();
	private final Set<Node<Stmt, Fact>> fieldContextReachable = Sets.newHashSet();
	private final Multimap<Node<Stmt, Fact>, AvailableListener> onFieldContextReachable = HashMultimap.create();
	private final Multimap<Node<Stmt, Fact>, AvailableListener> onCallingContextReachable = HashMultimap.create();
	private final Set<SyncPDSUpdateListener<Stmt, Fact, Field>> updateListeners = Sets.newHashSet();

	public SyncPDSSolver(){
		callAutomaton.registerListener(new CallAutomatonListener());
		fieldAutomaton.registerListener(new FieldUpdateListener());
		callingPDS.registerUpdateListener(new WPDSUpdateListener<Stmt, INode<Fact>, Weight<Stmt>>() {
			@Override
			public void onRuleAdded(Rule<Stmt, INode<Fact>, Weight<Stmt>> rule) {
				callingPDS.poststar(callAutomaton);
			}
		});	
		fieldPDS.registerUpdateListener(new WPDSUpdateListener<Field, INode<StmtWithFact>, Weight<Field>>() {
			@Override
			public void onRuleAdded(Rule<Field, INode<StmtWithFact>, Weight<Field>> rule) {
				fieldPDS.poststar(fieldAutomaton);
			}
		});
	}
	
	
	private class CallAutomatonListener implements WPAUpdateListener<Stmt, INode<Fact>,Weight<Stmt>>{

		@Override
		public void onAddedTransition(Transition<Stmt, INode<Fact>> t) {
		}

		@Override
		public void onWeightAdded(Transition<Stmt, INode<Fact>> t, Weight<Stmt> w) {
			if(!(t.getStart() instanceof GeneratedState)){
				if(w instanceof SetDomain && !w.equals(SetDomain.one()) && !w.equals(SetDomain.zero())){
					SetDomain<Stmt,Stmt,Fact> setDomain = (SetDomain) w;
					for(Node<Stmt,Fact> node : setDomain.elements()){
						setCallingContextReachable(new QueuedNode(t.getString(),t.getStart().fact(), node));
					}
				}
			}
		}
	}
	
	public void solve(Node<Stmt, Fact> curr) {
		Transition<Field, INode<SyncPDSSolver<Stmt, Fact, Field>.StmtWithFact>> fieldTrans = new Transition<Field, INode<StmtWithFact>>(asFieldFact(curr), epsilonField(), asFieldFact(curr));
		fieldAutomaton.addTransition(fieldTrans);
		fieldAutomaton.addWeightForTransition(fieldTrans, new SetDomain<Field,Stmt,Fact>(curr));
		Transition<Stmt, INode<Fact>> callTrans = new Transition<Stmt, INode<Fact>>(wrap(curr.fact()), curr.stmt(), wrap(curr.fact()));
		callAutomaton
				.addTransition(callTrans);
		callAutomaton.addWeightForTransition(callTrans, new SetDomain<Stmt,Stmt,Fact>(curr));
		worklist.add(new QueuedNode(curr,curr));
		awaitEmptyWorklist();
		if (DEBUG) {
			debugOutput();
		}
	}

	private void awaitEmptyWorklist() {
		while (!worklist.isEmpty()) {
			QueuedNode curr = worklist.poll();
			addReachableState(curr);

			Collection<? extends State> successors = computeSuccessor(curr.asNode());
			 System.out.println(curr+ " FLows tot \t\t\t "+successors);
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
					if (added)
						addToWorklist(new QueuedNode(succ,curr.source));
				} else if (s instanceof PopNode) {
					PopNode<Fact> popNode = (PopNode<Fact>) s;
					PDSSystem system = popNode.system();
					Fact location = popNode.location();
					processPop(curr, location, system);
				}
			}
		}
	}

	private void addReachableState(QueuedNode curr) {
		if (!reachedStates.add(curr))
			return;
		for (SyncPDSUpdateListener<Stmt, Fact, Field> l : updateListeners) {
			l.onReachableNodeAdded(curr);
		}
	}

	private void addToWorklist(QueuedNode curr) {
		worklist.add(curr);
	}

	private boolean processNormal(QueuedNode curr, Node<Stmt, Fact> succ) {
		boolean added = addNormalFieldFlow(curr, succ);
		added |= addNormalCallFlow(curr, succ);
		fieldPDS.poststar(fieldAutomaton);
		callingPDS.poststar(callAutomaton);
		return added;
	}

	private boolean addNormalCallFlow(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ) {
		return callingPDS.addRule(
				new NormalRule<Stmt, INode<Fact>,Weight<Stmt>>(wrap(curr.fact()), curr.stmt(), wrap(succ.fact()), succ.stmt(),callingPDS.getOne()));
	}

	private boolean addNormalFieldFlow(QueuedNode curr, Node<Stmt, Fact> succ) {
		setFieldContextReachable(new QueuedNode(succ,curr.source));
		if (succ instanceof ExclusionNode) {
			ExclusionNode<Stmt, Fact, Field> exNode = (ExclusionNode) succ;
			return fieldPDS.addRule(new NormalRule<Field, INode<StmtWithFact>, Weight<Field>>(asFieldFact(curr),
					fieldWildCard(), asFieldFact(succ), exclusionFieldWildCard(exNode.exclusion()), fieldPDS.getOne()));
		}
		return fieldPDS.addRule(new NormalRule<Field, INode<StmtWithFact>, Weight<Field>>(asFieldFact(curr),
				fieldWildCard(), asFieldFact(succ), fieldWildCard(), fieldPDS.getOne()));
	}

	public abstract Field exclusionFieldWildCard(Field exclusion);

	public abstract Field fieldWildCard();

	protected INode<StmtWithFact> asFieldFact(Node<Stmt, Fact> node) {
		return new SingleNode<StmtWithFact>(new StmtWithFact(node.stmt(), node.fact()));
	}

	private void processPop(QueuedNode curr, Fact location, PDSSystem system) {
		if (system.equals(PDSSystem.FIELDS)) {
			NodeWithLocation<Stmt, Fact, Field> node = (NodeWithLocation) location;
			fieldPDS.addRule(new PopRule<Field, INode<StmtWithFact>, Weight<Field>>(asFieldFact(curr), node.location(),
					asFieldFact(node.fact()), fieldPDS.getOne()));
			setCallingContextReachable(new QueuedNode(node.fact(),curr.source));
			addNormalCallFlow(curr, node.fact());
			checkFieldFeasibility(node.fact());
		} else if (system.equals(PDSSystem.CALLS)) {
			//
			callingPDS.addRule(new PopRule<Stmt, INode<Fact>, Weight<Stmt>>(wrap(curr.fact()), curr.stmt(), wrap((Fact) location),callingPDS.getOne()));
			checkCallFeasibility(curr, location);
		}
	}

	private boolean processPush(QueuedNode curr, Location location, Node<Stmt, Fact> succ, PDSSystem system) {
		boolean added = false;
		if (system.equals(PDSSystem.FIELDS)) {
			added |= fieldPDS.addRule(new PushRule<Field, INode<StmtWithFact>, Weight<Field>>(asFieldFact(curr),
					fieldWildCard(), asFieldFact(succ),  (Field) location,fieldWildCard(), fieldPDS.getOne()));
			added |= addNormalCallFlow(curr, succ);

			fieldPDS.poststar(fieldAutomaton);
		} else if (system.equals(PDSSystem.CALLS)) {
			added |= addNormalFieldFlow(curr, succ);
			added |= callingPDS.addRule(new PushRule<Stmt, INode<Fact>, Weight<Stmt>>(wrap(curr.fact()), curr.stmt(),
					wrap(succ.fact()), succ.stmt(), (Stmt) location,callingPDS.getOne()));

			// TODO This call is unnecessary, why?
			callingPDS.poststar(callAutomaton);
			addReturnSite((Stmt) location);
		}
		return added;
	}

	private void checkFieldFeasibility(Node<Stmt, Fact> node) {
		// System.out.println("CHECKING Field reachabilty for " + node);
		fieldPDS.poststar(fieldAutomaton);
	}

	private class FieldUpdateListener implements WPAUpdateListener<Field, INode<StmtWithFact>, Weight<Field>> {


		public FieldUpdateListener() {
		}

		@Override
		public void onAddedTransition(Transition<Field, INode<SyncPDSSolver<Stmt, Fact, Field>.StmtWithFact>> t) {
			
		}

		private SyncPDSSolver getOuterType() {
			return SyncPDSSolver.this;
		}

		@Override
		public void onWeightAdded(Transition<Field, INode<SyncPDSSolver<Stmt, Fact, Field>.StmtWithFact>> t,
				Weight<Field> w) {
			INode<StmtWithFact> n = t.getStart();
			if(!(n instanceof GeneratedState)){
				StmtWithFact fact = n.fact();
				if(w instanceof SetDomain) {
					SetDomain<Field,Stmt,Fact> setDomain = (SetDomain) w;
					for(Node<Stmt,Fact> allocNode : setDomain.elements()){
						setFieldContextReachable(new QueuedNode(fact.stmt(), fact.fact(),allocNode));
					}
				}
			}
		}

	}

	private void checkCallFeasibility(QueuedNode curr, Fact fact) {
		addReturningFact(curr, fact);
		callingPDS.poststar(callAutomaton);
	}

	private void addReturningFact(QueuedNode curr, Fact fact) {
		if (!returningFacts.put(curr, fact)) {
			return;
		}
		for (Stmt retSite : returnSites) {
			callAutomaton.registerListener(new CallUpdateListener(curr, retSite, fact));
		}
	}

	private void addReturnSite(Stmt location) {
		if (!returnSites.add(location)) {
			return;
		}
		for (Entry<QueuedNode, Fact> retSite : returningFacts.entries()) {
			callAutomaton.registerListener(new CallUpdateListener(retSite.getKey(), location, retSite.getValue()));
		}
	}

	private class CallUpdateListener implements WPAUpdateListener<Stmt, INode<Fact>, Weight<Stmt>> {
		private Stmt retSite;
		private Fact fact;
		private QueuedNode curr;

		public CallUpdateListener(QueuedNode curr, Stmt retSite, Fact fact) {
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

	public class QueuedNode extends Node<Stmt, Fact> implements AvailableListener {

		private final Node<Stmt, Fact> source;

		private QueuedNode(Stmt stmt, Fact variable, Node<Stmt,Fact> source) {
			super(stmt, variable);
			this.source = source;
		}


		private QueuedNode(Node<Stmt, Fact> node, Node<Stmt,Fact> source) {
			super(node.stmt(), node.fact());
			this.source = source;
		}

		@Override
		public void available() {
			addToWorklist(this);
		}

		public Node<Stmt, Fact> asNode() {
			return new Node<Stmt, Fact>(stmt(), fact());
		}
		
		public Node<Stmt,Fact> source(){
			return source;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((source == null) ? 0 : source.hashCode());
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
			QueuedNode other = (QueuedNode) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}


		@Override
		public String toString() {
			return "QueuedNode " + asNode().toString();
		}


		private SyncPDSSolver getOuterType() {
			return SyncPDSSolver.this;
		}
	}

	private void setCallingContextReachable(QueuedNode queuedNode) {
		Node<Stmt, Fact> node = queuedNode.asNode();
		if (!callingContextReachable.add(node))
			return;

		if (fieldContextReachable.contains(node)) {
			queuedNode.available();
			Collection<AvailableListener> listeners = onCallingContextReachable.removeAll(node);
			for (AvailableListener l : listeners) {
				l.available();
			}
		} else {
			onCallingContextReachable.put(node, queuedNode);
		}
	}

	private void setFieldContextReachable(QueuedNode queuedNode) {
		Node<Stmt, Fact> node = queuedNode.asNode();
		if (!fieldContextReachable.add(node)) {
			return;
		}
		// System.out.println("Set Field Context Reachable " + node);

		if (callingContextReachable.contains(node)) {
			queuedNode.available();
			Collection<AvailableListener> listeners = onFieldContextReachable.removeAll(node);
			for (AvailableListener l : listeners) {
				l.available();
			}
		} else {
			onFieldContextReachable.put(node, queuedNode);
		}
	}

	public void registerListener(SyncPDSUpdateListener<Stmt, Fact, Field> listener) {
		if (!updateListeners.add(listener)) {
			return;
		}
		for (QueuedNode reachableNode : reachedStates) {
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

	private class GeneratedState<L,N> implements INode<L>{
		
		private INode<L> node;
		private N loc;


		private GeneratedState(INode<L> node, N loc) {
			this.node = node;
			this.loc = loc;
		}
		@Override
		public L fact() {
			throw new RuntimeException("System internal state");
		}

		@Override
		public String toString() {
			return node + " " + loc;
		}
	}
	Map<Entry<INode<StmtWithFact>, Field>, INode<StmtWithFact>> generatedFieldState = Maps.newHashMap();

	protected INode<StmtWithFact> generateFieldState(final INode<StmtWithFact> d, final Field loc) {
		Entry<INode<StmtWithFact>, Field> e = new AbstractMap.SimpleEntry<>(d, loc);
		if (!generatedFieldState.containsKey(e)) {
			generatedFieldState.put(e, new GeneratedState<StmtWithFact,Field>(d,loc));
		}
		return generatedFieldState.get(e);
	}

	public abstract Collection<? extends State> computeSuccessor(Node<Stmt, Fact> node);

	public abstract Field epsilonField();

	public abstract Field emptyField();

	public abstract Stmt epsilonStmt();

	public Set<Node<Stmt, Fact>> getReachedStates() {
		Set<Node<Stmt,Fact>> res = Sets.newHashSet();
		for(QueuedNode s : reachedStates)
			res.add(s.asNode());
		return res;
	}

	private interface AvailableListener {
		void available();
	}

	public class StmtWithFact extends Node<Stmt, Fact> {

		public StmtWithFact(Stmt stmt, Fact variable) {
			super(stmt, variable);
		}

	}

	private void debugOutput() {
		System.out.println(this.getClass());
		System.out.println("All reachable states");
		prettyPrintSet(reachedStates);

		HashSet<Node<Stmt, Fact>> notFieldReachable = Sets.newHashSet(callingContextReachable);
		notFieldReachable.removeAll(reachedStates);
		HashSet<Node<Stmt, Fact>> notCallingContextReachable = Sets.newHashSet(fieldContextReachable);
		notCallingContextReachable.removeAll(reachedStates);
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
			System.out.print("\n");
			// if(j++ > 0){
			// System.out.print("\n");
			// j = 0;
			// }
		}
		System.out.println();
	}
}
