/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
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
import wpds.impl.*;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public abstract class SyncPDSSolver<Stmt extends Location, Fact, Field extends Location, W extends Weight> {

	public enum PDSSystem {
		FIELDS, CALLS
	}

	private static final boolean FieldSensitive = true;
	private static final boolean ContextSensitive = true;
	protected final WeightedPushdownSystem<Stmt, INode<Fact>, W> callingPDS = new WeightedPushdownSystem<Stmt, INode<Fact>, W>(){
		public String toString() {
			return "Call " + super.toString();
		};
	};
	protected final WeightedPushdownSystem<Field, INode<Node<Stmt,Fact>>, W> fieldPDS = new WeightedPushdownSystem<Field, INode<Node<Stmt,Fact>>, W>(){
		public String toString() {
			return "Field " + super.toString();
		};
	};
	private final Set<WitnessNode<Stmt,Fact,Field>> reachedStates = Sets.newHashSet();
	private final Set<Node<Stmt, Fact>> callingContextReachable = Sets.newHashSet();
	private final Set<Node<Stmt, Fact>> fieldContextReachable = Sets.newHashSet();
	private final Set<SyncPDSUpdateListener<Stmt, Fact, Field>> updateListeners = Sets.newHashSet();
	private final Multimap<WitnessNode<Stmt,Fact,Field>, SyncStatePDSUpdateListener<Stmt, Fact, Field>> reachedStateUpdateListeners = HashMultimap.create();
	protected final WeightedPAutomaton<Field, INode<Node<Stmt,Fact>>, W> fieldAutomaton;
	protected final WeightedPAutomaton<Stmt, INode<Fact>,W> callAutomaton;

	protected boolean preventFieldTransitionAdd(Transition<Field, INode<Node<Stmt, Fact>>> trans, W weight) {
		return false;
	}

	protected boolean preventCallTransitionAdd(Transition<Stmt, INode<Fact>> trans, W weight) {
		return false;
	}
	public SyncPDSSolver(INode<Fact> initialCallNode, INode<Node<Stmt,Fact>> initialFieldNode, final boolean useCallSummaries, NestedWeightedPAutomatons<Stmt, INode<Fact>, W> callSummaries,final boolean useFieldSummaries, NestedWeightedPAutomatons<Field, INode<Node<Stmt, Fact>>, W> fieldSummaries){
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
				return useFieldSummaries;
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
				return useCallSummaries;
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
		if(callAutomaton.nested())
			callAutomaton.registerNestedAutomatonListener(new CallSummaryListener());
//		if(fieldAutomaton.nested())
//			fieldAutomaton.registerNestedAutomatonListener(new FieldSummaryListener());
		callingPDS.poststar(callAutomaton,callSummaries);
		fieldPDS.poststar(fieldAutomaton,fieldSummaries);

	}

	private class FieldSummaryListener implements  NestedAutomatonListener<Field, INode<Node<Stmt, Fact>>, W>{
		@Override
		public void nestedAutomaton(final WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> parent,
				final WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> child) {
			child.registerListener(new FieldAddEpsilonToInitialStateListener(child.getInitialState(), parent));
		}
	}
	
	private class FieldAddEpsilonToInitialStateListener extends WPAStateListener<Field, INode<Node<Stmt, Fact>>, W>{

		private WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> parent;

		public FieldAddEpsilonToInitialStateListener(INode<Node<Stmt,Fact>> state, WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> parent) {
			super(state);
			this.parent = parent;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Stmt, Fact>>>t, W w,
				WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> weightedPAutomaton) {
		}

		@Override
		public void onInTransitionAdded(final Transition<Field, INode<Node<Stmt,Fact>>> nestedT, W w,
				WeightedPAutomaton<Field, INode<Node<Stmt, Fact>>, W> weightedPAutomaton) {
			if (nestedT.getLabel().equals(fieldAutomaton.epsilon())) {
				parent.registerListener(new FieldOnOutTransitionAddToStateListener(this.getState(), nestedT));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
			FieldAddEpsilonToInitialStateListener other = (FieldAddEpsilonToInitialStateListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (parent == null) {
				if (other.parent != null)
					return false;
			} else if (!parent.equals(other.parent))
				return false;
			return true;
		}

		private SyncPDSSolver getOuterType() {
			return SyncPDSSolver.this;
		}
	}
	private class FieldOnOutTransitionAddToStateListener extends WPAStateListener<Field, INode<Node<Stmt,Fact>>, W> {
		private Transition<Field, INode<Node<Stmt,Fact>>> nestedT;
		public FieldOnOutTransitionAddToStateListener(INode<Node<Stmt,Fact>> state, Transition<Field, INode<Node<Stmt,Fact>>> nestedT) {
			super(state);
			this.nestedT = nestedT;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Stmt,Fact>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Stmt,Fact>>, W> weightedPAutomaton) {
			setFieldContextReachable(nestedT.getStart().fact());
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Stmt,Fact>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Stmt,Fact>>, W> weightedPAutomaton) {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((nestedT == null) ? 0 : nestedT.hashCode());
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
			FieldOnOutTransitionAddToStateListener other = (FieldOnOutTransitionAddToStateListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (nestedT == null) {
				if (other.nestedT != null)
					return false;
			} else if (!nestedT.equals(other.nestedT))
				return false;
			return true;
		}

		private SyncPDSSolver getOuterType() {
			return SyncPDSSolver.this;
		}
	}
	private class CallSummaryListener implements NestedAutomatonListener<Stmt, INode<Fact>, W>{
		@Override
		public void nestedAutomaton(final WeightedPAutomaton<Stmt, INode<Fact>, W> parent,
				final WeightedPAutomaton<Stmt, INode<Fact>, W> child) {
			child.registerListener(new AddEpsilonToInitialStateListener(child.getInitialState(), parent));
		}
	}
	
	private class AddEpsilonToInitialStateListener extends WPAStateListener<Stmt, INode<Fact>, W>{

		private WeightedPAutomaton<Stmt, INode<Fact>, W> parent;

		public AddEpsilonToInitialStateListener(INode<Fact> state, WeightedPAutomaton<Stmt, INode<Fact>, W> parent) {
			super(state);
			this.parent = parent;
		}

		@Override
		public void onOutTransitionAdded(Transition<Stmt, INode<Fact>> t, W w,
				WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {
		}

		@Override
		public void onInTransitionAdded(final Transition<Stmt, INode<Fact>> nestedT, W w,
				WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {
			if (nestedT.getLabel().equals(callAutomaton.epsilon())) {
				parent.registerListener(new OnOutTransitionAddToStateListener(this.getState(), nestedT));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
			AddEpsilonToInitialStateListener other = (AddEpsilonToInitialStateListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (parent == null) {
				if (other.parent != null)
					return false;
			} else if (!parent.equals(other.parent))
				return false;
			return true;
		}

		private SyncPDSSolver getOuterType() {
			return SyncPDSSolver.this;
		}
	}
	private class OnOutTransitionAddToStateListener extends WPAStateListener<Stmt, INode<Fact>, W> {
		private Transition<Stmt, INode<Fact>> nestedT;
		public OnOutTransitionAddToStateListener(INode<Fact> state, Transition<Stmt, INode<Fact>> nestedT) {
			super(state);
			this.nestedT = nestedT;
		}

		@Override
		public void onOutTransitionAdded(Transition<Stmt, INode<Fact>> t, W w,
				WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {
			Node<Stmt, Fact> returningNode = new Node<Stmt, Fact>(t.getLabel(),
					nestedT.getStart().fact());
			setCallingContextReachable(returningNode);
		}

		@Override
		public void onInTransitionAdded(Transition<Stmt, INode<Fact>> t, W w,
				WeightedPAutomaton<Stmt, INode<Fact>, W> weightedPAutomaton) {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((nestedT == null) ? 0 : nestedT.hashCode());
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
			OnOutTransitionAddToStateListener other = (OnOutTransitionAddToStateListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (nestedT == null) {
				if (other.nestedT != null)
					return false;
			} else if (!nestedT.equals(other.nestedT))
				return false;
			return true;
		}

		private SyncPDSSolver getOuterType() {
			return SyncPDSSolver.this;
		}
	}
	

	private class CallAutomatonListener implements WPAUpdateListener<Stmt, INode<Fact>,W>{

		@Override
		public void onWeightAdded(Transition<Stmt, INode<Fact>> t, W w, WeightedPAutomaton<Stmt, INode<Fact>,W> aut) {
			if(!(t.getStart() instanceof GeneratedState)){
				Node<Stmt, Fact> node = new Node<Stmt,Fact>(t.getString(),t.getStart().fact());
				setCallingContextReachable(node);
			}
		}
	}

	public void solve(Node<Stmt, Fact> curr, W weight) {
		Transition<Field, INode<Node<Stmt,Fact>>> fieldTrans = new Transition<Field, INode<Node<Stmt,Fact>>>(asFieldFact(curr), emptyField(), fieldAutomaton.getInitialState());
		fieldAutomaton.addTransition(fieldTrans);
		Transition<Stmt, INode<Fact>> callTrans = createInitialCallTransition(curr);
		callAutomaton
				.addWeightForTransition(callTrans,weight);
		WitnessNode<Stmt, Fact, Field> startNode = new WitnessNode<>(curr.stmt(),curr.fact());
		callAutomaton.computeValues(callTrans, weight);
		processNode(startNode);
	}
	public void solve(Node<Stmt, Fact> curr) {
		solve(curr,getCallWeights().getOne());
	}

	private Transition<Stmt, INode<Fact>> createInitialCallTransition(Node<Stmt, Fact> curr){
		return new Transition<Stmt, INode<Fact>>(wrap(curr.fact()), curr.stmt(), callAutomaton.getInitialState());
	}
	
	protected void processNode(WitnessNode<Stmt, Fact,Field> witnessNode) {
		if(!addReachableState(witnessNode))
			return;
		Node<Stmt, Fact> curr = witnessNode.asNode();
		Collection<? extends State> successors = computeSuccessor(curr);
		for (State s : successors) {
			if (s instanceof Node) {
				Node<Stmt, Fact> succ = (Node<Stmt, Fact>) s;
				if (succ instanceof PushNode) {
					PushNode<Stmt, Fact, Location> pushNode = (PushNode<Stmt, Fact, Location>) succ;
					PDSSystem system = pushNode.system();
					Location location = pushNode.location();
					processPush(curr, location, pushNode, system);
				} else {
					processNormal(curr, succ);
				}
			} else if (s instanceof PopNode) {
				PopNode<Fact> popNode = (PopNode<Fact>) s;
				processPop(curr, popNode);
			}
		}
	}


	private boolean addReachableState(WitnessNode<Stmt,Fact,Field> curr) {
		if (reachedStates.contains(curr))
			return false;
		reachedStates.add(curr);
		for (SyncPDSUpdateListener<Stmt, Fact, Field> l : Lists.newLinkedList(updateListeners)) {
			l.onReachableNodeAdded(curr);
		}
		for(SyncStatePDSUpdateListener<Stmt, Fact, Field> l : Lists.newLinkedList(reachedStateUpdateListeners.get(curr))){
			l.reachable();
		}
		return true;
	}

	public void processNormal(Node<Stmt,Fact> curr, Node<Stmt, Fact> succ) {
		addNormalFieldFlow(curr, succ);
		addNormalCallFlow(curr, succ);
	}

	public void addNormalCallFlow(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ) {
		addCallRule(
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
	public void addNormalFieldFlow(final Node<Stmt,Fact> curr, final Node<Stmt, Fact> succ) {
		if(succ instanceof CastNode){
			addFieldRule(new NormalRule<Field, INode<Node<Stmt,Fact>>, W>(asFieldFact(curr),
					fieldWildCard(), asFieldFact(succ), fieldWildCard(), getFieldWeights().normal(curr,succ)){
				@Override
				public boolean canBeApplied(Transition<Field, INode<Node<Stmt, Fact>>> t, W weight) {
					return canCastBeApplied(curr,t,(CastNode)succ,weight);
				}
			});
			return;
		}
		if (succ instanceof ExclusionNode) {
			ExclusionNode<Stmt, Fact, Field> exNode = (ExclusionNode) succ;
			addFieldRule(new NormalRule<Field, INode<Node<Stmt,Fact>>, W>(asFieldFact(curr),
					fieldWildCard(), asFieldFact(succ), exclusionFieldWildCard(exNode.exclusion()), getFieldWeights().normal(curr,succ)));
			return;
		}
		addFieldRule(new NormalRule<Field, INode<Node<Stmt,Fact>>, W>(asFieldFact(curr),
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
				addFieldRule(new PopRule<Field, INode<Node<Stmt,Fact>>, W>(asFieldFact(curr), node.location(),
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
				addCallRule(new PopRule<Stmt, INode<Fact>, W>(wrap(curr.fact()), curr.stmt(), wrap((Fact) location),getCallWeights().pop(curr, returnSite)));
			}else{
				addNormalCallFlow(curr, new Node<Stmt,Fact>(returnSite,(Fact)location));
			}
		}
	}

	public void processPush(Node<Stmt,Fact> curr, Location location, Node<Stmt, Fact> succ, PDSSystem system) {
		if (system.equals(PDSSystem.FIELDS)) {
			
			if(FieldSensitive){
				if(!fieldContextReachable.contains(succ)){
					addFieldRule(new PushRule<Field, INode<Node<Stmt,Fact>>, W>(asFieldFact(curr),
							fieldWildCard(), asFieldFact(succ),  (Field) location,fieldWildCard(), getFieldWeights().push(curr,succ,(Field)location)));
				}
			} else{
				addNormalFieldFlow(curr, succ);
			}
			addNormalCallFlow(curr, succ);

		} else if (system.equals(PDSSystem.CALLS)) {
			addNormalFieldFlow(curr, succ);
			if(ContextSensitive){
				addCallRule(new PushRule<Stmt, INode<Fact>, W>(wrap(curr.fact()), curr.stmt(),
						wrap(succ.fact()), succ.stmt(), (Stmt) location, getCallWeights().push(curr, succ, (Stmt) location)));
			} else{
				addNormalCallFlow(curr, succ);
			}

		}
	}

	public void addCallRule(Rule<Stmt, INode<Fact>,W> rule){
		callingPDS.addRule(rule);
	}

	public void addFieldRule(Rule<Field, INode<Node<Stmt,Fact>>, W> rule){
		fieldPDS.addRule(rule);
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
				setFieldContextReachable(node);
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
