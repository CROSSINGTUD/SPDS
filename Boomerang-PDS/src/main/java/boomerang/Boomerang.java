package boomerang;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import boomerang.customize.BackwardEmptyCalleeFlow;
import boomerang.customize.EmptyCalleeFlow;
import boomerang.customize.ForwardEmptyCalleeFlow;
import boomerang.debugger.Debugger;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.poi.AbstractPOI;
import boomerang.poi.PointOfIndirection;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import boomerang.solver.MethodBasedFieldTransitionListener;
import boomerang.solver.ReachableMethodListener;
import boomerang.solver.StatementBasedFieldTransitionListener;
import heros.utilities.DefaultValueMap;
import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.nodes.AllocNode;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.ConnectPushListener;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public abstract class Boomerang<W extends Weight> {
	public static final boolean DEBUG = true;
	private Map<Entry<INode<Node<Statement,Val>>, Field>, INode<Node<Statement,Val>>> genField = new HashMap<>();
	private final DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers = new DefaultValueMap<Query, AbstractBoomerangSolver<W>>() {
		@Override
		protected AbstractBoomerangSolver<W> createItem(Query key) {
			AbstractBoomerangSolver<W> solver;
			if (key instanceof BackwardQuery){
				System.out.println("Backward solving query: " + key);
				solver = createBackwardSolver((BackwardQuery) key);
			} else {
				System.out.println("Forward solving query: " + key);
				solver = createForwardSolver((ForwardQuery) key);
			}
			if(key.getType() instanceof RefType){
				addAllocationType((RefType) key.getType());
			}
			for(RefType type : allocatedTypes){
				solver.addAllocatedType(type);
			}
			
			for(ReachableMethodListener<W> l : reachableMethodsListener){
				solver.registerReachableMethodListener(l);
			}
			return solver;
		}
	};
	private BackwardsInterproceduralCFG bwicfg;
	private Collection<ForwardQuery> forwardQueries = Sets.newHashSet();
	private Collection<BackwardQuery> backwardQueries = Sets.newHashSet();
	private EmptyCalleeFlow forwardEmptyCalleeFlow = new ForwardEmptyCalleeFlow(); 
	private EmptyCalleeFlow backwardEmptyCalleeFlow = new BackwardEmptyCalleeFlow(); 
	private Collection<RefType> allocatedTypes = Sets.newHashSet();
	private Collection<ReachableMethodListener<W>> reachableMethodsListener = Sets.newHashSet();
	private Multimap<BackwardQuery, ForwardQuery> backwardToForwardQueries = HashMultimap.create();
	private Map<Transition<Statement, INode<Val>>, WeightedPAutomaton<Statement, INode<Val>, W>> backwardCallSummaries = Maps.newHashMap();
	private Map<Transition<Field, INode<Node<Statement, Val>>>, WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W>> backwardFieldSummaries = Maps.newHashMap();
	private Map<Transition<Statement, INode<Val>>, WeightedPAutomaton<Statement, INode<Val>, W>> forwardCallSummaries = Maps.newHashMap();
	private Map<Transition<Field, INode<Node<Statement, Val>>>, WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W>> forwardFieldSummaries = Maps.newHashMap();
	private DefaultValueMap<FieldWritePOI, FieldWritePOI> fieldWrites = new DefaultValueMap<FieldWritePOI, FieldWritePOI>() {
		@Override
		protected FieldWritePOI createItem(FieldWritePOI key) {
			return key;
		}
	};
	private DefaultValueMap<FieldReadPOI, FieldReadPOI> fieldReads = new DefaultValueMap<FieldReadPOI, FieldReadPOI>() {
		@Override
		protected FieldReadPOI createItem(FieldReadPOI key) {
			return key;
		}
	};
	private DefaultValueMap<ForwardCallSitePOI, ForwardCallSitePOI> forwardCallSitePOI = new DefaultValueMap<ForwardCallSitePOI, ForwardCallSitePOI>() {
		@Override
		protected ForwardCallSitePOI createItem(ForwardCallSitePOI key) {
			return key;
		}
	};
	protected AbstractBoomerangSolver<W> createBackwardSolver(final BackwardQuery backwardQuery) {
		final BackwardBoomerangSolver<W> solver = new BackwardBoomerangSolver<W>(bwicfg(), backwardQuery, genField, backwardCallSummaries, backwardFieldSummaries){

			@Override
			protected void callBypass(Statement callSite, Statement returnSite, Val value) {
			}

			@Override
			protected Collection<? extends State> getEmptyCalleeFlow(SootMethod caller, Stmt callSite, Val value,
					Stmt returnSite) {
				return backwardEmptyCalleeFlow.getEmptyCalleeFlow(caller, callSite, value, returnSite);
			}

			@Override
			protected void onReturnFromCall(final Statement callSite, Statement returnSite, 
					final Node<Statement, Val> returnedNode, final boolean unbalanced) {
				final ForwardCallSitePOI callSitePoi = forwardCallSitePOI.getOrCreate(new ForwardCallSitePOI(callSite));
				callSitePoi.returnsFromCall(backwardQuery, returnedNode);
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getFieldWeights() {
				return Boomerang.this.getBackwardFieldWeights();
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getCallWeights() {
				return Boomerang.this.getBackwardCallWeights();
			}
			
		};
		solver.registerListener(new SyncPDSUpdateListener<Statement, Val, Field>() {
			@Override
			public void onReachableNodeAdded(WitnessNode<Statement, Val, Field> node) {
				if(isAllocationNode(node.stmt(), node.fact())){
					forwardSolve(new ForwardQuery(node.stmt(),
							getAllocatedVal(node.stmt())),backwardQuery);
				}
				if(isFieldStore(node.stmt())){
				} else if(isArrayStore(node.stmt())){
//					forwardHandleFieldWrite(node, createArrayFieldStore(node.stmt()), sourceQuery);
				} else if(isFieldLoad(node.stmt())){
					backwardHandleFieldRead(node, createFieldLoad(node.stmt()), backwardQuery);
				}
				if(isBackwardEnterCall(node.stmt())){
					backwardHandleEnterCall(node, forwardCallSitePOI.getOrCreate(new ForwardCallSitePOI(node.stmt())), backwardQuery);
				}
			}

		});
		
		return solver;
	}
	
	protected void backwardHandleEnterCall(WitnessNode<Statement, Val, Field> node, ForwardCallSitePOI returnSite,
			BackwardQuery backwardQuery) {
		returnSite.returnsFromCall(backwardQuery, node.asNode());
	}

	protected boolean isBackwardEnterCall(Statement stmt) {
		return icfg().isExitStmt(stmt.getUnit().get());
	}

	private void forwardSolve(final ForwardQuery forwardQuery, BackwardQuery backwardQuery) {
		backwardToForwardQueries.put(backwardQuery, forwardQuery);
		forwardSolve(forwardQuery);
		queryToSolvers.getOrCreate(backwardQuery).addCallAutomatonListener(new WPAUpdateListener<Statement, INode<Val>, W>() {

			@Override
			public void onWeightAdded(Transition<Statement, INode<Val>> t, W w) {
				if(t.getTarget() instanceof GeneratedState){
					GeneratedState<Val,Statement> generatedState = (GeneratedState) t.getTarget();
//					queryToSolvers.getOrCreate(forwardQuery).addUnbalancedFlow(generatedState.location().getMethod(), Collections.empty);
				}
			}
		});
	}

	protected static Val getAllocatedVal(Statement s) {
		AssignStmt optUnit = (AssignStmt) s.getUnit().get();
		return new Val(optUnit.getLeftOp(), s.getMethod());
	}

	protected static boolean isAllocationNode(Statement s, Val fact) {
		Optional<Stmt> optUnit = s.getUnit();
		if (optUnit.isPresent()) {
			Stmt stmt = optUnit.get();
			if (stmt instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) stmt;
				if (as.getLeftOp().equals(fact.value()) && isAllocationVal(as.getRightOp())) {
					return true;
				}
			}
		}
		return false;
	}

	protected AbstractBoomerangSolver<W> createForwardSolver(final ForwardQuery sourceQuery) {
		ForwardBoomerangSolver<W> solver = new ForwardBoomerangSolver<W>(icfg(), sourceQuery,genField, forwardCallSummaries, forwardFieldSummaries){
			@Override
			protected void onReturnFromCall(Statement callSite, Statement returnSite, final Node<Statement, Val> returnedNode, final boolean unbalanced) {
				Boomerang.this.onForwardReturnFromCall(callSite, returnedNode, sourceQuery);
			}
			
			@Override
			protected void callBypass(Statement callSite, Statement returnSite, Val value) {
				ForwardCallSitePOI callSitePoi = forwardCallSitePOI.getOrCreate(new ForwardCallSitePOI(callSite));
				callSitePoi.addByPassingAllocation(sourceQuery);
			}

			@Override
			protected Collection<? extends State> getEmptyCalleeFlow(SootMethod caller, Stmt callSite, Val value,
					Stmt returnSite) {
				return forwardEmptyCalleeFlow.getEmptyCalleeFlow(caller, callSite, value, returnSite);
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getCallWeights() {
				return Boomerang.this.getForwardCallWeights();
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getFieldWeights() {
				return Boomerang.this.getForwardFieldWeights();
			}
		};
		
		solver.registerListener(new SyncPDSUpdateListener<Statement, Val, Field>() {
			@Override
			public void onReachableNodeAdded(WitnessNode<Statement, Val, Field> node) {
				if(isFieldStore(node.stmt())){
					forwardHandleFieldWrite(node, createFieldStore(node.stmt()), sourceQuery);
				} else if(isArrayStore(node.stmt())){
					forwardHandleFieldWrite(node, createArrayFieldStore(node.stmt()), sourceQuery);
				} else if(isFieldLoad(node.stmt())){
					forwardHandleFieldLoad(node, createFieldLoad(node.stmt()), sourceQuery);
				}
				if(isBackwardEnterCall(node.stmt())){
					forwardCallSitePOI.getOrCreate(new ForwardCallSitePOI(node.stmt())).addByPassingAllocation(sourceQuery);
				}
			}
		});
		solver.getCallAutomaton().registerConnectPushListener(new ConnectPushListener<Statement, INode<Val>, W>() {

			@Override
			public void connect(Statement callSite, Statement returnSite, INode<Val> returnedFact, W returnedWeight) {
				final ForwardCallSitePOI callSitePoi = forwardCallSitePOI.getOrCreate(new ForwardCallSitePOI(callSite));
				callSitePoi.returnsFromCall(sourceQuery, new Node<Statement,Val>(returnSite, returnedFact.fact()));
			}
		});
		return solver;
	}
	
	protected void onForwardReturnFromCall(Statement callSite, Node<Statement, Val> returnedNode, Query sourceQuery){
	}

	protected FieldReadPOI createFieldLoad(Statement s) {	
		Stmt stmt = s.getUnit().get();
		AssignStmt as = (AssignStmt) stmt;
		InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
		Val base = new Val(ifr.getBase(), icfg().getMethodOf(as));
		Field field = new Field(ifr.getField());
		return fieldReads.getOrCreate(new FieldReadPOI(s, base,field, new Val(as.getLeftOp(), icfg().getMethodOf(as))));
	}

	protected FieldWritePOI createArrayFieldStore(Statement s) {
		Stmt stmt = s.getUnit().get();
		AssignStmt as = (AssignStmt) stmt;
		ArrayRef ifr = (ArrayRef) as.getLeftOp();
		Val base = new Val(ifr.getBase(), icfg().getMethodOf(as));
		Val stored = new Val(as.getRightOp(), icfg().getMethodOf(as));
		return fieldWrites.getOrCreate(new FieldWritePOI(s, base, Field.array(), stored));
	}

	protected FieldWritePOI createFieldStore(Statement s) {
		Stmt stmt = s.getUnit().get();
		AssignStmt as = (AssignStmt) stmt;
		InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
		Val base = new Val(ifr.getBase(), icfg().getMethodOf(as));
		Val stored = new Val(as.getRightOp(), icfg().getMethodOf(as));
		Field field = new Field(ifr.getField());
		return fieldWrites.getOrCreate(new FieldWritePOI(s, base, field, stored));
	}



	public static boolean isFieldStore(Statement s) {
		Optional<Stmt> optUnit = s.getUnit();
		if (optUnit.isPresent()) {
			Stmt stmt = optUnit.get();
			if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() instanceof InstanceFieldRef) {
				return true;
			}
		}
		return false;
	}


	public static boolean isArrayStore(Statement s) {
		Optional<Stmt> optUnit = s.getUnit();
		if (optUnit.isPresent()) {
			Stmt stmt = optUnit.get();
			if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() instanceof ArrayRef) {
				return true;
			}
		}
		return false;
	}

	public static boolean isFieldLoad(Statement s) {
		Optional<Stmt> optUnit = s.getUnit();
		if (optUnit.isPresent()) {
			Stmt stmt = optUnit.get();
			if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getRightOp() instanceof InstanceFieldRef) {
				return true;
			}
		}
		return false;
	}



	protected void backwardHandleFieldRead(final WitnessNode<Statement, Val, Field> node, FieldReadPOI fieldRead, final BackwardQuery sourceQuery) {	
		BackwardQuery backwardQuery = new BackwardQuery(node.stmt(),
				fieldRead.getBaseVar());
		if (node.fact().equals(fieldRead.getStoredVar())) {
//			backwardSolve(backwardQuery);
			fieldRead.addFlowAllocation(sourceQuery);
		}
	}

	public static boolean isAllocationVal(Value val) {
		return val instanceof NullConstant || val instanceof NewExpr || val instanceof NewArrayExpr || val instanceof NewMultiArrayExpr;
	}

	protected void forwardHandleFieldWrite(final WitnessNode<Statement, Val, Field> node, final FieldWritePOI fieldWritePoi, final ForwardQuery sourceQuery) {
		BackwardQuery backwardQuery = new BackwardQuery(node.stmt(),
				fieldWritePoi.getBaseVar());
		if (node.fact().equals(fieldWritePoi.getStoredVar())) {
			backwardSolve(backwardQuery);
			fieldWritePoi.addFlowAllocation(sourceQuery);
		}
		if (node.fact().equals(fieldWritePoi.getBaseVar())) {
			queryToSolvers.getOrCreate(sourceQuery).getFieldAutomaton().registerListener(new TriggerBaseAllocationAtFieldWrite(new SingleNode<Node<Statement,Val>>(node.asNode()),fieldWritePoi,sourceQuery));
		}
	}
	
	private class TriggerBaseAllocationAtFieldWrite extends WPAStateListener<Field, INode<Node<Statement, Val>>, W>{

		private final PointOfIndirection<Statement, Val, Field> fieldWritePoi;
		private final ForwardQuery sourceQuery;

		public TriggerBaseAllocationAtFieldWrite(INode<Node<Statement, Val>> state, PointOfIndirection<Statement, Val, Field> fieldWritePoi, ForwardQuery sourceQuery) {
			super(state);
			this.fieldWritePoi = fieldWritePoi;
			this.sourceQuery = sourceQuery;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w) {
			if(t.getTarget().fact().equals(sourceQuery.asNode())){
				fieldWritePoi.addBaseAllocation(sourceQuery);
			}
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w) {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((fieldWritePoi == null) ? 0 : fieldWritePoi.hashCode());
			result = prime * result + ((sourceQuery == null) ? 0 : sourceQuery.hashCode());
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
			TriggerBaseAllocationAtFieldWrite other = (TriggerBaseAllocationAtFieldWrite) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (fieldWritePoi == null) {
				if (other.fieldWritePoi != null)
					return false;
			} else if (!fieldWritePoi.equals(other.fieldWritePoi))
				return false;
			if (sourceQuery == null) {
				if (other.sourceQuery != null)
					return false;
			} else if (!sourceQuery.equals(other.sourceQuery))
				return false;
			return true;
		}

		private Boomerang getOuterType() {
			return Boomerang.this;
		}
		
	}

	private void forwardHandleFieldLoad(final WitnessNode<Statement, Val, Field> node, final FieldReadPOI fieldReadPoi, final ForwardQuery sourceQuery) {
		if (node.fact().equals(fieldReadPoi.getBaseVar())) {
			queryToSolvers.getOrCreate(sourceQuery).registerFieldTransitionListener(new MethodBasedFieldTransitionListener<W>(node.stmt().getMethod()) {
				@Override
				public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
					if(t.getStart().fact().equals(node.asNode()) && t.getTarget().fact().equals(sourceQuery.asNode())){
						fieldReadPoi.addBaseAllocation(sourceQuery);
					}
				}
			});
		}
	}


	private BiDiInterproceduralCFG<Unit, SootMethod> bwicfg() {
		if (bwicfg == null)
			bwicfg = new BackwardsInterproceduralCFG(icfg());
		return bwicfg;
	}

	public void solve(Query query) {
		if (query instanceof ForwardQuery) {
			forwardSolve((ForwardQuery) query);
		}
		if (query instanceof BackwardQuery) {
			backwardSolve((BackwardQuery) query);
		}
	}
	

	protected void backwardSolve(BackwardQuery query) {
		backwardQueries.add(query);
		Optional<Stmt> unit = query.asNode().stmt().getUnit();
		AbstractBoomerangSolver<W> solver = queryToSolvers.getOrCreate(query);
		if (unit.isPresent()) {
			for (Unit succ : new BackwardsInterproceduralCFG(icfg()).getSuccsOf(unit.get())) {
				solver.solve(query.asNode(), new Node<Statement, Val>(
						new Statement((Stmt) succ, icfg().getMethodOf(succ)), query.asNode().fact()));
			}
		}
	}

	private void forwardSolve(ForwardQuery query) {
		Optional<Stmt> unit = query.asNode().stmt().getUnit();
		forwardQueries.add(query);
		AbstractBoomerangSolver<W> solver = queryToSolvers.getOrCreate(query);
		if (unit.isPresent()) {
			for (Unit succ : icfg().getSuccsOf(unit.get())) {
				Node<Statement, Val> source = new Node<Statement, Val>(
						new Statement((Stmt) succ, icfg().getMethodOf(succ)), query.asNode().fact());
				if(isMultiArrayAllocation(unit.get())){
					solver.getFieldAutomaton().addTransition(new Transition<Field,INode<Node<Statement,Val>>>(new SingleNode<Node<Statement,Val>>(source),Field.array(),new AllocNode<Node<Statement,Val>>(query.asNode())));
					solver.getFieldAutomaton().addTransition(new Transition<Field,INode<Node<Statement,Val>>>(new SingleNode<Node<Statement,Val>>(source),Field.array(),new SingleNode<Node<Statement,Val>>(source)));
				}
				solver.solve(query.asNode(), source);
			}
		}
	}

	private boolean isMultiArrayAllocation(Stmt stmt) {
		return (stmt instanceof AssignStmt) && ((AssignStmt)stmt).getRightOp() instanceof NewMultiArrayExpr;
	}

	private class FieldWritePOI extends FieldStmtPOI {
		public FieldWritePOI(Statement statement, Val base, Field field, Val stored) {
			super(statement,base,field,stored);
		}

		@Override
		public void execute(final ForwardQuery baseAllocation, final Query flowAllocation) {
			if(flowAllocation instanceof BackwardQuery){
			} else if(flowAllocation instanceof ForwardQuery){
				executeImportAliases(baseAllocation, flowAllocation);
			}
		}
	}
	private class QueryWithVal{
		private final Query flowSourceQuery;
		private final Node<Statement, Val> returningFact;

		QueryWithVal(Query q, Node<Statement, Val> asNode){
			this.flowSourceQuery = q;
			this.returningFact = asNode;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((flowSourceQuery == null) ? 0 : flowSourceQuery.hashCode());
			result = prime * result + ((returningFact== null) ? 0 : returningFact.hashCode());
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
			QueryWithVal other = (QueryWithVal) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (flowSourceQuery == null) {
				if (other.flowSourceQuery != null)
					return false;
			} else if (!flowSourceQuery.equals(other.flowSourceQuery))
				return false;
			if (returningFact == null) {
				if (other.returningFact != null)
					return false;
			} else if (!returningFact.equals(other.returningFact))
				return false;
			return true;
		}

		private Boomerang<W> getOuterType() {
			return Boomerang.this;
		}

	}
	
	
	private class ForwardCallSitePOI {
		private Statement callSite;
		private Set<QueryWithVal> returnsFromCall = Sets.newHashSet();
		private Set<ForwardQuery> byPassingAllocations = Sets.newHashSet();
		
		public ForwardCallSitePOI(Statement callSite){
			this.callSite = callSite;
		}
		private Multimap<Query,Query> importGraph = HashMultimap.create();

		private boolean introducesLoop(ForwardQuery baseAllocation, Query flowAllocation) {
			importGraph.put(baseAllocation, flowAllocation);
			LinkedList<Query> worklist = Lists.newLinkedList();
			worklist.add(flowAllocation);
			Set<Query> visited = Sets.newHashSet();
			while(!worklist.isEmpty()){
				Query curr = worklist.pop();
				if(visited.add(curr)){
					worklist.addAll(importGraph.get(curr));
				}
			}
			return visited.contains(baseAllocation);
		}

		public void returnsFromCall(final Query flowQuery, Node<Statement, Val> returnedNode) {
			if(returnsFromCall.add(new QueryWithVal(flowQuery, returnedNode))){
				for(final ForwardQuery byPassing : Lists.newArrayList(byPassingAllocations)){
					eachPair(byPassing, flowQuery, returnedNode);
				}
			}
		}

		private void eachPair(final ForwardQuery byPassing, final Query flowQuery, final Node<Statement,Val> returnedNode){
			if(byPassing.equals(flowQuery)) 
				return;
			if(introducesLoop(byPassing, flowQuery))
				return;
			queryToSolvers.getOrCreate(flowQuery).registerFieldTransitionListener(new MethodBasedFieldTransitionListener<W>(byPassing.asNode().stmt().getMethod()) {
				private boolean triggered = false;
				@Override
				public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
					if(!triggered && t.getStart().fact().equals(byPassing.asNode())){
						triggered = true;
						queryToSolvers.getOrCreate(byPassing).registerListener(new SyncPDSUpdateListener<Statement, Val, Field>() {

							@Override
							public void onReachableNodeAdded(WitnessNode<Statement, Val, Field> reachableNode) {
								if(reachableNode.asNode().equals(returnedNode)){
									importFlowsAtReturnSite(byPassing, flowQuery, returnedNode);
								}
							}
						});
					}
				}
			});
		}
		protected void importFlowsAtReturnSite(final ForwardQuery byPassingAllocation, final Query flowQuery, final Node<Statement, Val> returnedNode) {
			final AbstractBoomerangSolver<W> byPassingSolver = queryToSolvers.get(byPassingAllocation);
		    final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> byPassingFieldAutomaton = byPassingSolver.getFieldAutomaton();
		    final AbstractBoomerangSolver<W> flowSolver = queryToSolvers.getOrCreate(flowQuery);
//		    System.out.println(flowQuery + " " + callSite + " -> "+ returnedNode.stmt() + byPassingAllocation);
		    byPassingSolver.registerStatementFieldTransitionListener(new StatementBasedFieldTransitionListener<W>(returnedNode.stmt()) {
		    	@Override
				public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
					if(!(t.getStart() instanceof GeneratedState)){
						Val byPassing = t.getStart().fact().fact();
//						if(byPassing.equals(returnedNode.fact()))
//							return;
//						if(flowSolver.getFieldAutomaton().containsTransitions(t.getStart()))
//							return;
						byPassingFieldAutomaton.registerListener(new ImportToSolver(t.getStart(), byPassingSolver, flowSolver));
						flowSolver.setFieldContextReachable(new Node<Statement,Val>(returnedNode.stmt(),byPassing));
						flowSolver.addNormalCallFlow(returnedNode,  new Node<Statement,Val>(returnedNode.stmt(),byPassing));
					}
				}
			});
		}

		public void addByPassingAllocation(ForwardQuery byPassingAllocation) {
			if(byPassingAllocations.add(byPassingAllocation)){
				for(QueryWithVal e : Lists.newArrayList(returnsFromCall)){
					eachPair(byPassingAllocation, e.flowSourceQuery,e.returningFact);
				}
			}
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
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
			ForwardCallSitePOI other = (ForwardCallSitePOI) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (callSite == null) {
				if (other.callSite != null)
					return false;
			} else if (!callSite.equals(other.callSite))
				return false;
			return true;
		}

		private Boomerang<W> getOuterType() {
			return Boomerang.this;
		}
		
	}
	private class FieldReadPOI extends FieldStmtPOI {
		public FieldReadPOI(Statement statement, Val base, Field field, Val stored) {
			super(statement,base,field,stored);
		}
		
		@Override
		public void execute(final ForwardQuery baseAllocation, final Query flowAllocation) {
			if(Boomerang.this instanceof WholeProgramBoomerang)
				throw new RuntimeException("should not be invoked!");
			if(flowAllocation instanceof ForwardQuery){
			} else if(flowAllocation instanceof BackwardQuery){
				executeImportAliases(baseAllocation, flowAllocation);
			}
		}
	}
	private abstract class FieldStmtPOI extends AbstractPOI<Statement, Val, Field> {
		private Multimap<Query,Query> importGraph = HashMultimap.create();
		public FieldStmtPOI(Statement statement, Val base, Field field, Val storedVar) {
			super(statement, base, field, storedVar);
		}

		private boolean introducesLoop(ForwardQuery baseAllocation, Query flowAllocation) {
			importGraph.put(baseAllocation, flowAllocation);
			LinkedList<Query> worklist = Lists.newLinkedList();
			worklist.add(flowAllocation);
			Set<Query> visited = Sets.newHashSet();
			while(!worklist.isEmpty()){
				Query curr = worklist.pop();
				if(visited.add(curr)){
					worklist.addAll(importGraph.get(curr));
				}
			}
			return visited.contains(baseAllocation);
		}
		protected void executeImportAliases(final ForwardQuery baseAllocation, final Query flowAllocation){
			final AbstractBoomerangSolver<W> baseSolver = queryToSolvers.get(baseAllocation);
			final AbstractBoomerangSolver<W> flowSolver = queryToSolvers.get(flowAllocation);
			if(introducesLoop(baseAllocation, flowAllocation)){
				return;
			}
			assert !flowSolver.getSuccsOf(getStmt()).isEmpty();
			baseSolver.registerStatementFieldTransitionListener(new StatementBasedFieldTransitionListener<W>(getStmt()) {
			
				@Override
				public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
					final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
					if(!(aliasedVariableAtStmt instanceof GeneratedState)){
						Val alias = aliasedVariableAtStmt.fact().fact();
						final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut = baseSolver.getFieldAutomaton();
						aut.registerListener(new ImportToSolver(t.getTarget(), baseSolver, flowSolver));
						
						for(final Statement succOfWrite : flowSolver.getSuccsOf(getStmt())){
							Node<Statement, Val> aliasedVarAtSucc = new Node<Statement,Val>(succOfWrite,alias);
							flowSolver.getFieldAutomaton().addTransition(new Transition<Field, INode<Node<Statement,Val>>>(new AllocNode<Node<Statement,Val>>(baseAllocation.asNode()), Field.epsilon(), new SingleNode<Node<Statement,Val>>(new Node<Statement,Val>(succOfWrite,getBaseVar()))));
							flowSolver.getFieldAutomaton().addTransition(new Transition<Field, INode<Node<Statement,Val>>>(new SingleNode<Node<Statement,Val>>(aliasedVarAtSucc), t.getLabel(),t.getTarget()));
							Node<Statement, Val> rightOpNode = new Node<Statement, Val>(getStmt(),getStoredVar());
							flowSolver.setFieldContextReachable(aliasedVarAtSucc);
							flowSolver.addNormalCallFlow(rightOpNode,aliasedVarAtSucc);
						}	

					}
				}
			});
		}
		
	}
	class ImportToSolver extends WPAStateListener<Field, INode<Node<Statement,Val>>,W>{
		private AbstractBoomerangSolver<W> flowSolver;
		private AbstractBoomerangSolver<W> baseSolver;
		public ImportToSolver(INode<Node<Statement, Val>> state, AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver) {
			super(state);
			this.baseSolver = baseSolver;
			this.flowSolver = flowSolver;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
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
			ImportToSolver other = (ImportToSolver) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (flowSolver == null) {
				if (other.flowSolver != null)
					return false;
			} else if (!flowSolver.equals(other.flowSolver))
				return false;
			return true;
		}
		
		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w) {
			if(flowSolver.getFieldAutomaton().addTransition(t)){
				baseSolver.getFieldAutomaton().registerListener(new ImportToSolver(t.getTarget(), baseSolver, flowSolver));
			}
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w) {
		}


		private Boomerang getOuterType() {
			return Boomerang.this;
		}
	}
	
	public boolean addAllocationType(RefType type){
		if(allocatedTypes.add(type)){
			for(AbstractBoomerangSolver<W> solvers : queryToSolvers.values()){
				solvers.addAllocatedType(type);
			}
			return true;
		}
		return false;
	}

	public void registerReachableMethodListener(ReachableMethodListener<W> l){
		if(reachableMethodsListener.add(l)){
			for(AbstractBoomerangSolver<W> s : queryToSolvers.values()){
				s.registerReachableMethodListener(l);
			}
		}
	}
	public abstract BiDiInterproceduralCFG<Unit, SootMethod> icfg();
	protected abstract WeightFunctions<Statement, Val, Field, W> getForwardFieldWeights();
	protected abstract WeightFunctions<Statement, Val, Field, W> getBackwardFieldWeights();
	protected abstract WeightFunctions<Statement, Val, Statement, W> getBackwardCallWeights();
	protected abstract WeightFunctions<Statement, Val, Statement, W> getForwardCallWeights();
	
	
	public Collection<? extends Node<Statement, Val>> getForwardReachableStates() {
		Set<Node<Statement, Val>> res = Sets.newHashSet();
		for (Query q : queryToSolvers.keySet()) {
			if (q instanceof ForwardQuery)
				res.addAll(queryToSolvers.getOrCreate(q).getReachedStates());
		}
		return res;
	}
	public DefaultValueMap<Query, AbstractBoomerangSolver<W>> getSolvers() {
		return queryToSolvers;
	}
	
	public abstract Debugger<W> createDebugger();


	public void debugOutput() {
		for (Query q : queryToSolvers.keySet()) {
			System.out.println(q +" Nodes: " + queryToSolvers.getOrCreate(q).getReachedStates().size());
			System.out.println(q +" Field Aut: " + queryToSolvers.getOrCreate(q).getFieldAutomaton().getTransitions().size());
			System.out.println(q +" Field Aut (failed Additions): " + queryToSolvers.getOrCreate(q).getFieldAutomaton().failedAdditions);
			System.out.println(q +" Field Aut (failed Direct Additions): " + queryToSolvers.getOrCreate(q).getFieldAutomaton().failedDirectAdditions);
			System.out.println(q +" Call Aut: " + queryToSolvers.getOrCreate(q).getCallAutomaton().getTransitions().size());
			System.out.println(q +" Call Aut (failed Additions): " + queryToSolvers.getOrCreate(q).getCallAutomaton().failedAdditions);
		}
		if(!DEBUG)
			return;
		Debugger<W> debugger = createDebugger();
		for (Query q : queryToSolvers.keySet()) {
			debugger.reachableNodes(q,queryToSolvers.getOrCreate(q).getTransitionsToFinalWeights());
			debugger.reachableCallNodes(q,queryToSolvers.getOrCreate(q).getReachedStates());
			debugger.reachableFieldNodes(q,queryToSolvers.getOrCreate(q).getReachedStates());
			
//			if (q instanceof ForwardQuery) {
				System.out.println("========================");
				System.out.println(q);
				System.out.println("========================");
				 queryToSolvers.getOrCreate(q).debugOutput();
				 for(FieldReadPOI  p : fieldReads.values()){
					 queryToSolvers.getOrCreate(q).debugFieldAutomaton(p.getStmt());
					 for(Statement succ :  queryToSolvers.getOrCreate(q).getSuccsOf(p.getStmt())){
						 queryToSolvers.getOrCreate(q).debugFieldAutomaton(succ);
					 }
				 }
				 for(FieldWritePOI  p : fieldWrites.values()){
					 queryToSolvers.getOrCreate(q).debugFieldAutomaton(p.getStmt());
					 for(Statement succ :  queryToSolvers.getOrCreate(q).getSuccsOf(p.getStmt())){
						 queryToSolvers.getOrCreate(q).debugFieldAutomaton(succ);
					 }
				 }
//			}
		}
		// backwardSolver.debugOutput();
		// forwardSolver.debugOutput();
		// for(ForwardQuery fq : forwardQueries){
		// for(Node<Statement, Val> bq : forwardSolver.getReachedStates()){
		// IRegEx<Field> extractLanguage =
		// forwardSolver.getFieldAutomaton().extractLanguage(new
		// SingleNode<Node<Statement,Val>>(bq),new
		// SingleNode<Node<Statement,Val>>(fq.asNode()));
		// System.out.println(bq + " "+ fq +" "+ extractLanguage);
		// }
		// }
		// for(final BackwardQuery bq : backwardQueries){
		// forwardSolver.synchedEmptyStackReachable(bq.asNode(), new
		// WitnessListener<Statement, Val>() {
		//
		// @Override
		// public void witnessFound(Node<Statement, Val> targetFact) {
		// System.out.println(bq + " is allocated at " +targetFact);
		// }
		// });
		// }

		// for(ForwardQuery fq : forwardQueries){
		// System.out.println(fq);
		// System.out.println(Joiner.on("\n\t").join(forwardSolver.getFieldAutomaton().dfs(new
		// SingleNode<Node<Statement,Val>>(fq.asNode()))));
		// }
	}
}
