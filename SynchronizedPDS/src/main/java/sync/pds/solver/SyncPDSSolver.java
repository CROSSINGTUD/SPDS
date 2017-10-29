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

import sync.pds.solver.nodes.AllocNode;
import sync.pds.solver.nodes.CallPopNode;
import sync.pds.solver.nodes.CastNode;
import sync.pds.solver.nodes.ExclusionNode;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.NestedAutomatonListener;
import wpds.impl.NormalRule;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.impl.WeightedPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public abstract class SyncPDSSolver<Stmt extends Location, Fact, Field extends Location, W extends Weight> {

	public enum PDSSystem {
		FIELDS, CALLS
	}

	private LinkedList<WitnessNode<Stmt,Fact,Field>> worklist = Lists.newLinkedList();
	private static final boolean FieldSensitive = true;
	private static final boolean ContextSensitive = true;
	protected final WeightedPushdownSystem<Stmt, INode<Fact>, W> callingPDS = new WeightedPushdownSystem<Stmt, INode<Fact>, W>();
	protected final WeightedPushdownSystem<Field, INode<Node<Stmt,Fact>>, W> fieldPDS = new WeightedPushdownSystem<Field, INode<Node<Stmt,Fact>>, W>();
	protected final Map<Transition<Stmt, INode<Fact>>, W> nodesToWeights = Maps.newHashMap(); 
	private final Set<WitnessNode<Stmt,Fact,Field>> reachedStates = Sets.newHashSet();
	private final Set<Node<Stmt, Fact>> callingContextReachable = Sets.newHashSet();
	private final Set<Node<Stmt, Fact>> callingContextSilentReachable = Sets.newHashSet();
	private final Set<Node<Stmt, Fact>> fieldContextReachable = Sets.newHashSet();
	private final Set<SyncPDSUpdateListener<Stmt, Fact, Field>> updateListeners = Sets.newHashSet();
	private final Multimap<WitnessNode<Stmt,Fact,Field>, SyncStatePDSUpdateListener<Stmt, Fact, Field>> reachedStateUpdateListeners = HashMultimap.create();
	protected final WeightedPAutomaton<Field, INode<Node<Stmt,Fact>>, W> fieldAutomaton;
	protected final WeightedPAutomaton<Stmt, INode<Fact>,W> callAutomaton;

	public SyncPDSSolver(INode<Fact> initialCallNode, INode<Node<Stmt,Fact>> initialFieldNode){
		this(initialCallNode, initialFieldNode, Maps.<INode<Fact>, WeightedPAutomaton<Stmt, INode<Fact>, W>>newHashMap(),Maps.<INode<Node<Stmt, Fact>>, WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W>>newHashMap());
	}
	
	protected boolean preventFieldTransitionAdd(Transition<Field, INode<Node<Stmt, Fact>>> trans, W weight) {
		return false;
	}

	protected boolean preventCallTransitionAdd(Transition<Stmt, INode<Fact>> trans, W weight) {
		return false;
	}
	public SyncPDSSolver(INode<Fact> initialCallNode, INode<Node<Stmt,Fact>> initialFieldNode, Map<INode<Fact>, WeightedPAutomaton<Stmt, INode<Fact>, W>> callSummaries,Map<INode<Node<Stmt, Fact>>, WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W>> fieldSummaries){
		fieldAutomaton = new WeightedPAutomaton<Field, INode<Node<Stmt,Fact>>, W>(initialFieldNode) {
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

			@Override
			public boolean nested() {
				return false;
			};
			
			@Override
			public W getZero() {
				return getFieldWeights().getZero();
			}

			@Override
			public W getOne() {
				return getFieldWeights().getOne();
			}
			public boolean addWeightForTransition(Transition<Field,INode<Node<Stmt,Fact>>> trans, W weight) {
				if(preventFieldTransitionAdd(trans,weight))
					return false;
				return super.addWeightForTransition(trans, weight);
			};
			@Override
			public boolean isGeneratedState(INode<Node<Stmt, Fact>> d) {
				return d instanceof GeneratedState;
			}
		};

		callAutomaton = new WeightedPAutomaton<Stmt, INode<Fact>,W>(initialCallNode) {
			@Override
			public INode<Fact> createState(INode<Fact> d, Stmt loc) {
				return generateCallState(d, loc);
			}

			@Override
			public Stmt epsilon() {
				return epsilonStmt();
			}

			@Override
			public W getZero() {
				return getCallWeights().getZero();
			}

			@Override
			public boolean nested() {
				return true;
			};
			@Override
			public W getOne() {
				return getCallWeights().getOne();
			}
			
			public boolean addWeightForTransition(Transition<Stmt,INode<Fact>> trans, W weight) {
				if(preventCallTransitionAdd(trans,weight))
					return false;
				return super.addWeightForTransition(trans, weight);
			};

			@Override
			public boolean isGeneratedState(INode<Fact> d) {
				return d instanceof GeneratedState;
			}
		};
		
		callAutomaton.registerListener(new CallAutomatonListener());
		fieldAutomaton.registerListener(new FieldUpdateListener());
		callingPDS.poststar(callAutomaton,callSummaries);
		fieldPDS.poststar(fieldAutomaton,fieldSummaries);
	}
	
	

	private class CallAutomatonListener implements WPAUpdateListener<Stmt, INode<Fact>,W>{

		@Override
		public void onWeightAdded(Transition<Stmt, INode<Fact>> t, W w, WeightedPAutomaton<Stmt, INode<Fact>,W> aut) {
			if(!(t.getStart() instanceof GeneratedState)){
				Node<Stmt, Fact> node = new Node<Stmt,Fact>(t.getString(),t.getStart().fact());
				if(callAutomaton.nested() && !aut.isInitialAutomaton(callAutomaton)){
					callingContextSilentReachable.add(node);
					callAutomaton.registerNestedAutomatonListener(new NestedAutomatonListener<Stmt, INode<Fact>, W>() {

						@Override
						public void nestedAutomaton(final WeightedPAutomaton<Stmt, INode<Fact>, W> parent,
								final WeightedPAutomaton<Stmt, INode<Fact>, W> child) {
							child.registerListener(new WPAStateListener<Stmt, INode<Fact>, W>(child.getInitialState()) {
								@Override
								public void onOutTransitionAdded(Transition<Stmt, INode<Fact>> t, W w,
										WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {
									
								}

								@Override
								public void onInTransitionAdded(final Transition<Stmt, INode<Fact>> nestedT, W w,
										WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {
									if(nestedT.getLabel().equals(callAutomaton.epsilon())){
										parent.registerListener(new WPAStateListener<Stmt, INode<Fact>, W>(child.getInitialState()) {

											@Override
											public void onOutTransitionAdded(Transition<Stmt, INode<Fact>> t, W w,
													WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {
												Node<Stmt, Fact> returningNode = new Node<Stmt,Fact>(t.getLabel(),nestedT.getStart().fact());
//												callingContextSilentReachable.add(returningNode);
												setCallingContextReachable(returningNode);
											}

											@Override
											public void onInTransitionAdded(Transition<Stmt, INode<Fact>> t, W w,
													WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {
												
											}
										});
									}
								}
							});
						}
					});
				}else{
					setCallingContextReachable(node);
				}
			}
		}
	}

	public void solve(Node<Stmt,Fact> source) {
		solve(source,source);
	}
	
	public void solve(Node<Stmt,Fact> source, Node<Stmt, Fact> curr) {
		solve(source,curr,callAutomaton.getOne());
	}
	
	public void solve(Node<Stmt,Fact> source, Node<Stmt, Fact> curr,  W weight) {
		Transition<Field, INode<Node<Stmt,Fact>>> fieldTrans = new Transition<Field, INode<Node<Stmt,Fact>>>(asFieldFact(curr), emptyField(), asFieldFactSource(source));
		fieldAutomaton.addTransition(fieldTrans);
		Transition<Stmt, INode<Fact>> callTrans = new Transition<Stmt, INode<Fact>>(wrap(curr.fact()), curr.stmt(), wrap(source.fact()));
		callAutomaton
				.addTransition(callTrans);
		callAutomaton.setInitialState(wrap(source.fact()));
		fieldAutomaton.setInitialState(asFieldFactSource(source));
		WitnessNode<Stmt, Fact, Field> startNode = new WitnessNode<>(curr.stmt(),curr.fact());
		computeValues(callTrans, weight);
		processNode(startNode);
	}
	private void computeValues(Transition<Stmt, INode<Fact>> callTrans, W weight) {
		callAutomaton.computeValues(callTrans,weight);
	}

	
	private void await() {
		while(!worklist.isEmpty()){
			WitnessNode<Stmt, Fact, Field> pop = worklist.pop();
			processNode(pop);
		}
	}

	private INode<Node<Stmt, Fact>> asFieldFactSource(Node<Stmt, Fact> source) {
		return new AllocNode<Node<Stmt,Fact>>(source);
	}

	protected void processNode(WitnessNode<Stmt, Fact,Field> witnessNode) {
		if(!addReachableState(witnessNode))
			return;
		Node<Stmt, Fact> curr = witnessNode.asNode();
		Collection<? extends State> successors = computeSuccessor(curr);
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
					if(!reachedStates.contains(new WitnessNode<Stmt,Fact,Field>(succ.stmt(),succ.fact())))
						worklist.add(new WitnessNode<Stmt,Fact,Field>(succ.stmt(),succ.fact()));
				}
			} else if (s instanceof PopNode) {
				PopNode<Fact> popNode = (PopNode<Fact>) s;
				processPop(curr, popNode);
			}
		}
		await();
	}


	private boolean addReachableState(WitnessNode<Stmt,Fact,Field> curr) {
		if (reachedStates.contains(curr))
			return false;
//		System.out.println(this.getClass() + " " + curr);
		reachedStates.add(curr);
		for (SyncPDSUpdateListener<Stmt, Fact, Field> l : Lists.newLinkedList(updateListeners)) {
			l.onReachableNodeAdded(curr);
		}
		for(SyncStatePDSUpdateListener<Stmt, Fact, Field> l : Lists.newLinkedList(reachedStateUpdateListeners.get(curr))){
			l.reachable();
		}
		return true;
	}

	public boolean processNormal(Node<Stmt,Fact> curr, Node<Stmt, Fact> succ) {
		boolean added = addNormalFieldFlow(curr, succ);
		added |= addNormalCallFlow(curr, succ);
		return added;
	}

	public boolean addNormalCallFlow(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ) {

		if(callingContextSilentReachable.contains(curr)){
			System.out.println("Not adding "+  	new NormalRule<Stmt, INode<Fact>,W>(wrap(curr.fact()), curr.stmt(), wrap(succ.fact()), succ.stmt(),getCallWeights().normal(curr,succ)));
			return false;
		}
		return callingPDS.addRule(
				new NormalRule<Stmt, INode<Fact>,W>(wrap(curr.fact()), curr.stmt(), wrap(succ.fact()), succ.stmt(),getCallWeights().normal(curr,succ)));
	}

	public void synchedEmptyStackReachable(final Node<Stmt,Fact> sourceNode, final EmptyStackWitnessListener<Stmt,Fact> listener){
		synchedReachable(sourceNode,new WitnessListener<Stmt, Fact, Field>() {
			Multimap<Fact, Node<Stmt,Fact>> potentialFieldCandidate = HashMultimap.create();
			Set<Fact> potentialCallCandidate = Sets.newHashSet();
			@Override
			public void fieldWitness(Transition<Field, INode<Node<Stmt, Fact>>> t) {
				if(t.getTarget() instanceof GeneratedState)
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
			public void callWitness(Transition<Stmt, INode<Fact>> t) {
				if(t.getTarget() instanceof GeneratedState)
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
		});
	}
	public void synchedReachable(final Node<Stmt,Fact> sourceNode, final WitnessListener<Stmt,Fact,Field> listener){
		registerListener(new SyncPDSUpdateListener<Stmt, Fact, Field>() {
			@Override
			public void onReachableNodeAdded(WitnessNode<Stmt, Fact, Field> reachableNode) {
				if(!reachableNode.asNode().equals(sourceNode))
					return;
				fieldAutomaton.registerListener(new WPAUpdateListener<Field, INode<Node<Stmt,Fact>>, W>() {
					@Override
					public void onWeightAdded(Transition<Field, INode<Node<Stmt, Fact>>> t, W w, WeightedPAutomaton<Field, INode<Node<Stmt,Fact>>, W> aut) {
						if(t.getStart() instanceof GeneratedState)
							return;
						if(!t.getStart().fact().equals(sourceNode))
							return;
						listener.fieldWitness(t);
					}
				});
				callAutomaton.registerListener(new WPAUpdateListener<Stmt, INode<Fact>, W>() {
					@Override
					public void onWeightAdded(Transition<Stmt, INode<Fact>> t, W w, WeightedPAutomaton<Stmt, INode<Fact>,W> aut) {
						if(t.getStart() instanceof GeneratedState)
							return;
						if(!t.getStart().fact().equals(sourceNode.fact()))
							return;
						if(!t.getLabel().equals(sourceNode.stmt()))
							return;
						listener.callWitness(t);
					}
				});
			}
		});
	}
	public boolean addNormalFieldFlow(final Node<Stmt,Fact> curr, final Node<Stmt, Fact> succ) {
		if(succ instanceof CastNode){
			return fieldPDS.addRule(new NormalRule<Field, INode<Node<Stmt,Fact>>, W>(asFieldFact(curr),
					fieldWildCard(), asFieldFact(succ), fieldWildCard(), getFieldWeights().normal(curr,succ)){
				@Override
				public boolean canBeApplied(Transition<Field, INode<Node<Stmt, Fact>>> t, W weight) {
					return canCastBeApplied(curr,t,(CastNode)succ,weight);
				}
			});
		}
		if (succ instanceof ExclusionNode) {
			ExclusionNode<Stmt, Fact, Field> exNode = (ExclusionNode) succ;
			return fieldPDS.addRule(new NormalRule<Field, INode<Node<Stmt,Fact>>, W>(asFieldFact(curr),
					fieldWildCard(), asFieldFact(succ), exclusionFieldWildCard(exNode.exclusion()), getFieldWeights().normal(curr,succ)));
		}
		return fieldPDS.addRule(new NormalRule<Field, INode<Node<Stmt,Fact>>, W>(asFieldFact(curr),
				fieldWildCard(), asFieldFact(succ), fieldWildCard(), getFieldWeights().normal(curr,succ)));
	}


	protected boolean canCastBeApplied(Node<Stmt, Fact> curr, Transition<Field, INode<Node<Stmt, Fact>>> t, CastNode<Stmt, Fact,?> succ, W weight){
		return true;
	}
	protected INode<Node<Stmt,Fact>> asFieldFact(Node<Stmt, Fact> node) {
		return new SingleNode<Node<Stmt,Fact>>(new Node<Stmt,Fact>(node.stmt(), node.fact()));
	}

	public void processPop(Node<Stmt,Fact> curr, PopNode popNode) {
		PDSSystem system = popNode.system();
		Object location = popNode.location();
		if (system.equals(PDSSystem.FIELDS)) {
			NodeWithLocation<Stmt, Fact, Field> node = (NodeWithLocation) location;
			if(FieldSensitive){
				fieldPDS.addRule(new PopRule<Field, INode<Node<Stmt,Fact>>, W>(asFieldFact(curr), node.location(),
						asFieldFact(node.fact()), getFieldWeights().pop(curr, node.location())));
			} else{
				addNormalFieldFlow(curr, node.fact());
			}
			addNormalCallFlow(curr, node.fact());
		} else if (system.equals(PDSSystem.CALLS)) {
			//TODO we have an unchecked cast here, branch directly based on PopNode type?
			CallPopNode<Fact, Stmt> callPopNode = (CallPopNode) popNode;
			Stmt returnSite = callPopNode.getReturnSite();
			addNormalFieldFlow(curr, new Node<Stmt,Fact>(returnSite,(Fact)location));
			if(ContextSensitive){
				callingPDS.addRule(new PopRule<Stmt, INode<Fact>, W>(wrap(curr.fact()), curr.stmt(), wrap((Fact) location),getCallWeights().pop(curr, curr.stmt())));
			}else{
				addNormalCallFlow(curr, new Node<Stmt,Fact>(returnSite,(Fact)location));
			}
		}
	}

	public boolean processPush(Node<Stmt,Fact> curr, Location location, Node<Stmt, Fact> succ, PDSSystem system) {
		boolean added = false;
		if (system.equals(PDSSystem.FIELDS)) {
			if(FieldSensitive){
			added |= fieldPDS.addRule(new PushRule<Field, INode<Node<Stmt,Fact>>, W>(asFieldFact(curr),
					fieldWildCard(), asFieldFact(succ),  (Field) location,fieldWildCard(), getFieldWeights().push(curr,succ,(Field)location)));
			} else{
				added |= addNormalFieldFlow(curr, succ);
			}
			added |= addNormalCallFlow(curr, succ);

		} else if (system.equals(PDSSystem.CALLS)) {
			added |= addNormalFieldFlow(curr, succ);
			if(ContextSensitive){
				added |= callingPDS.addRule(new PushRule<Stmt, INode<Fact>, W>(wrap(curr.fact()), curr.stmt(),
						wrap(succ.fact()), succ.stmt(), (Stmt) location, getCallWeights().push(curr, succ, (Stmt) location)));
			} else{
				added |= addNormalCallFlow(curr, succ);
			}

		}
		return added;
	}
	

	protected abstract WeightFunctions<Stmt, Fact, Field, W> getFieldWeights();
	
	protected abstract WeightFunctions<Stmt, Fact, Stmt, W> getCallWeights();

	private class FieldUpdateListener implements WPAUpdateListener<Field, INode<Node<Stmt,Fact>>, W> {

		@Override
		public void onWeightAdded(Transition<Field, INode<Node<Stmt,Fact>>> t,
				W w, WeightedPAutomaton<Field, INode<Node<Stmt,Fact>>, W> aut) {
			INode<Node<Stmt,Fact>> n = t.getStart();
			if(!(n instanceof GeneratedState)){
				Node<Stmt,Fact> fact = n.fact();
				Node<Stmt, Fact> node = new Node<Stmt,Fact>(fact.stmt(), fact.fact());
//				if(aut.equals(fieldAutomaton) && fieldAutomaton.nested()){
//					fieldContextReachable.add(node);
//				} else {
					setFieldContextReachable(node);
//				}
			}
		}
	}


	public void setCallingContextReachable(Node<Stmt,Fact> node) {
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
	public void setFieldContextReachable(Node<Stmt,Fact> node) {
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
		for (WitnessNode<Stmt, Fact, Field> reachableNode : Lists.newArrayList(reachedStates)) {
			listener.onReachableNodeAdded(reachableNode);
		}
	}
	public void registerListener(SyncStatePDSUpdateListener<Stmt, Fact, Field> listener) {
		if (!reachedStateUpdateListeners.put(listener.getNode(), listener)){
			return;
		}
		if(reachedStates.contains(listener.getNode())){
			listener.reachable();
		}
	}

	protected INode<Fact> wrap(Fact variable) {
		return new SingleNode<Fact>(variable);
	}

	Map<Entry<INode<Fact>, Stmt>, INode<Fact>> generatedCallState = Maps.newHashMap();

	public INode<Fact> generateCallState(final INode<Fact> d, final Stmt loc) {
		Entry<INode<Fact>, Stmt> e = new AbstractMap.SimpleEntry<>(d, loc);
		if (!generatedCallState.containsKey(e)) {
			generatedCallState.put(e, new GeneratedState<Fact,Stmt>(d,loc));
		}
		return generatedCallState.get(e);
	}

	Map<Entry<INode<Node<Stmt,Fact>>, Field>, INode<Node<Stmt,Fact>>> generatedFieldState = Maps.newHashMap();

	public INode<Node<Stmt,Fact>> generateFieldState(final INode<Node<Stmt,Fact>> d, final Field loc) {
		Entry<INode<Node<Stmt,Fact>>, Field> e = new AbstractMap.SimpleEntry<>(d, loc);
		if (!generatedFieldState.containsKey(e)) {
			generatedFieldState.put(e, new GeneratedState<Node<Stmt,Fact>,Field>(d,loc));
		}
		return generatedFieldState.get(e);
	}
	

	public void addGeneratedFieldState(GeneratedState<Node<Stmt,Fact>,Field> state) {
		Entry<INode<Node<Stmt,Fact>>, Field> e = new AbstractMap.SimpleEntry<>(state.node(), state.location());
		generatedFieldState.put(e,state);
	}

	public abstract Collection<? extends State> computeSuccessor(Node<Stmt, Fact> node);

	public abstract Field epsilonField();

	public abstract Field emptyField();

	public abstract Stmt epsilonStmt();
	
	public abstract Field exclusionFieldWildCard(Field exclusion);

	public abstract Field fieldWildCard();

	public Set<Node<Stmt, Fact>> getReachedStates() {
		Set<Node<Stmt,Fact>> res = Sets.newHashSet();
		for(WitnessNode<Stmt, Fact, Field> s : reachedStates)
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
			System.out.println("Calling context reachable");
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
