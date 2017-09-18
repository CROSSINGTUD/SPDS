package boomerang;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import boomerang.solver.ReachableMethodListener;
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
import sync.pds.solver.EmptyStackWitnessListener;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.nodes.AllocNode;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.ForwardDFSVisitor;
import wpds.interfaces.ReachabilityListener;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;

public abstract class Boomerang {
	public static final boolean DEBUG = true;
	private static final boolean DISABLE_CALLPOI = false;
	Map<Entry<INode<Node<Statement,Val>>, Field>, INode<Node<Statement,Val>>> genField = new HashMap<>();
	private final DefaultValueMap<Query, AbstractBoomerangSolver> queryToSolvers = new DefaultValueMap<Query, AbstractBoomerangSolver>() {
		@Override
		protected AbstractBoomerangSolver createItem(Query key) {
			AbstractBoomerangSolver solver;
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
			
			for(ReachableMethodListener l : reachableMethodsListener){
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
	private Collection<ReachableMethodListener> reachableMethodsListener = Sets.newHashSet();
	private Multimap<BackwardQuery, ForwardQuery> backwardToForwardQueries = HashMultimap.create();
	private DefaultValueMap<FieldWritePOI, FieldWritePOI> fieldWrites = new DefaultValueMap<FieldWritePOI, FieldWritePOI>() {
		@Override
		protected FieldWritePOI createItem(FieldWritePOI key) {
			return key;
		}
	};
//	private DefaultValueMap<BackwardFieldWritePOI, BackwardFieldWritePOI> backwardFieldWrites = new DefaultValueMap<BackwardFieldWritePOI, BackwardFieldWritePOI>() {
//		@Override
//		protected BackwardFieldWritePOI createItem(BackwardFieldWritePOI key) {
//			return key;
//		}
//	};
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
	protected AbstractBoomerangSolver createBackwardSolver(final BackwardQuery backwardQuery) {
		final BackwardBoomerangSolver solver = new BackwardBoomerangSolver(bwicfg(), backwardQuery,genField){

			@Override
			protected void callBypass(Statement callSite, Statement returnSite, Val value) {
				// TODO Auto-generated method stub
			}

			@Override
			protected Collection<? extends State> getEmptyCalleeFlow(SootMethod caller, Stmt callSite, Val value,
					Stmt returnSite) {
				return backwardEmptyCalleeFlow.getEmptyCalleeFlow(caller, callSite, value, returnSite);
			}
			
		};
		solver.registerListener(new SyncPDSUpdateListener<Statement, Val, Field>() {
			@Override
			public void onReachableNodeAdded(WitnessNode<Statement, Val, Field> node) {
				Optional<Stmt> optUnit = node.stmt().getUnit();
				if (optUnit.isPresent()) {
					Stmt stmt = optUnit.get();
					if (stmt instanceof AssignStmt) {
						AssignStmt as = (AssignStmt) stmt;
						if (node.fact().value().equals(as.getLeftOp()) && isAllocationVal(as.getRightOp())) {
							final ForwardQuery forwardQuery = new ForwardQuery(node.stmt(),
									new Val(as.getLeftOp(), icfg().getMethodOf(stmt)));
							backwardToForwardQueries.put(backwardQuery, forwardQuery);
							forwardSolve(forwardQuery);
							queryToSolvers.getOrCreate(backwardQuery).addCallAutomatonListener(new WPAUpdateListener<Statement, INode<Val>, Weight<Statement>>() {

								@Override
								public void onAddedTransition(Transition<Statement, INode<Val>> t) {
									if(t.getTarget() instanceof GeneratedState){
										GeneratedState<Val,Statement> generatedState = (GeneratedState) t.getTarget();
										queryToSolvers.getOrCreate(forwardQuery).addUnbalancedFlow(generatedState.location().getMethod());
									}
								}

								@Override
								public void onWeightAdded(Transition<Statement, INode<Val>> t, Weight<Statement> w) {
									
								}
							});
						}

						if (as.getRightOp() instanceof InstanceFieldRef) {
							InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
							handleFieldRead(node, fieldReads.getOrCreate(new FieldReadPOI(node.stmt(), new Val(ifr.getBase(),node.stmt().getMethod()), new Field(ifr.getField()), new Val(as.getLeftOp(),node.stmt().getMethod()))), backwardQuery);
						}
					}
				}
			}

		});
		
		return solver;
	}


	protected AbstractBoomerangSolver createForwardSolver(final ForwardQuery sourceQuery) {
		ForwardBoomerangSolver solver = new ForwardBoomerangSolver(icfg(), sourceQuery,genField){
			@Override
			protected void onReturnFromCall(Statement callSite, Statement returnSite, final Node<Statement, Val> predecessor, final Node<Statement, Val> asNode) {
				final ForwardCallSitePOI callSitePoi = forwardCallSitePOI.getOrCreate(new ForwardCallSitePOI(callSite,returnSite));
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, Weight<Field>> aut = queryToSolvers.getOrCreate(sourceQuery).getFieldAutomaton();
				aut.registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, Weight<Field>>() {

					@Override
					public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
						if(t.getStart() instanceof AllocNode){
							Node<Statement, Val> fact = t.getStart().fact();
							if(!fact.equals(sourceQuery.asNode())){
								callSitePoi.addFlowAllocation(sourceQuery, new ForwardQuery(fact.stmt(),fact.fact()),asNode.fact());
							}
						}
					}

					@Override
					public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, Weight<Field> w) {
						
					}
				});
				
			}
			
			@Override
			protected void callBypass(Statement callSite, Statement returnSite, Val value) {
				ForwardCallSitePOI callSitePoi = forwardCallSitePOI.getOrCreate(new ForwardCallSitePOI(callSite,returnSite));
//				System.out.println("Query + " + sourceQuery + " bypasses " + callSite);
				callSitePoi.addByPassingAllocation(sourceQuery,value);
			}

			@Override
			protected Collection<? extends State> getEmptyCalleeFlow(SootMethod caller, Stmt callSite, Val value,
					Stmt returnSite) {
				return forwardEmptyCalleeFlow.getEmptyCalleeFlow(caller, callSite, value, returnSite);
			}
			
		};
		
		solver.registerListener(new SyncPDSUpdateListener<Statement, Val, Field>() {
			@Override
			public void onReachableNodeAdded(WitnessNode<Statement, Val, Field> node) {
				Optional<Stmt> optUnit = node.stmt().getUnit();
				if (optUnit.isPresent()) {
					Stmt stmt = optUnit.get();
					if (stmt instanceof AssignStmt) {
						AssignStmt as = (AssignStmt) stmt;
						if (as.getLeftOp() instanceof InstanceFieldRef) {
							InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
							final Val base = new Val(ifr.getBase(), icfg().getMethodOf(as));
							final Val stored = new Val(as.getRightOp(), icfg().getMethodOf(as));
							final Field field = new Field(ifr.getField());
							handleFieldWrite(node, fieldWrites.getOrCreate(new FieldWritePOI(node.stmt(), base, field, stored)), sourceQuery);
						}
						
						if (as.getLeftOp() instanceof ArrayRef) {
							ArrayRef ifr = (ArrayRef) as.getLeftOp();
							Val base = new Val(ifr.getBase(), icfg().getMethodOf(as));
							Val stored = new Val(as.getRightOp(), icfg().getMethodOf(as));
							handleFieldWrite(node, fieldWrites.getOrCreate(new FieldWritePOI(node.stmt(), base, Field.array(), stored)), sourceQuery);
						}
						if (as.getRightOp() instanceof InstanceFieldRef) {
							InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
							attachHandlerFieldRead(node, ifr, as, sourceQuery);
						}
					}
				}
			}
		});
		return solver;
	}

	protected void handleFieldRead(final WitnessNode<Statement, Val, Field> node, FieldReadPOI fieldRead, final BackwardQuery sourceQuery) {	
		BackwardQuery backwardQuery = new BackwardQuery(node.stmt(),
				fieldRead.getBaseVar());
		if (node.fact().equals(fieldRead.getStoredVar())) {
			addBackwardQuery(backwardQuery, new EmptyStackWitnessListener<Statement, Val>() {
				@Override
				public void witnessFound(Node<Statement, Val> alloc) {
				}
			});
			fieldRead.addFlowAllocation(sourceQuery);
		}
	}

	public static boolean isAllocationVal(Value val) {
		return val instanceof NullConstant || val instanceof NewExpr || val instanceof NewArrayExpr || val instanceof NewMultiArrayExpr;
	}

	protected void handleFieldWrite(final WitnessNode<Statement, Val, Field> node, final FieldWritePOI fieldWritePoi, final ForwardQuery sourceQuery) {
		BackwardQuery backwardQuery = new BackwardQuery(node.stmt(),
				fieldWritePoi.getBaseVar());
		if (node.fact().equals(fieldWritePoi.getStoredVar())) {
			addBackwardQuery(backwardQuery, new EmptyStackWitnessListener<Statement, Val>() {
				@Override
				public void witnessFound(Node<Statement, Val> alloc) {
				}
			});
			fieldWritePoi.addFlowAllocation(sourceQuery);
		}
		if (node.fact().equals(fieldWritePoi.getBaseVar())) {
			queryToSolvers.getOrCreate(sourceQuery).addFieldAutomatonListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, Weight<Field>>() {
				@Override
				public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
					if(t.getStart().fact().equals(node.asNode()) && t.getTarget().fact().equals(sourceQuery.asNode())){
						fieldWritePoi.addBaseAllocation(sourceQuery);
					}
				}
				@Override
				public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, Weight<Field> w) {
				}
			});
		}
	}

	private void attachHandlerFieldRead(final WitnessNode<Statement, Val, Field> node, final InstanceFieldRef ifr,
			final AssignStmt fieldRead, final ForwardQuery sourceQuery) {
		Val base = new Val(ifr.getBase(), icfg().getMethodOf(fieldRead));
		final Field field = new Field(ifr.getField());
		final FieldReadPOI fieldReadPoi = 	fieldReads.getOrCreate(new FieldReadPOI(node.stmt(), base,field, new Val(fieldRead.getLeftOp(), icfg().getMethodOf(fieldRead))));
		if (node.fact().value().equals(ifr.getBase())) {
			queryToSolvers.getOrCreate(sourceQuery).getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, Weight<Field>>() {

				@Override
				public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
					if(t.getStart().fact().equals(node.asNode()) && t.getTarget().fact().equals(sourceQuery.asNode())){
						//TODO only if empty field.
						fieldReadPoi.addBaseAllocation(sourceQuery);
					}
				}

				@Override
				public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, Weight<Field> w) {
					
				}
			});
		}
	}


	private BiDiInterproceduralCFG<Unit, SootMethod> bwicfg() {
		if (bwicfg == null)
			bwicfg = new BackwardsInterproceduralCFG(icfg());
		return bwicfg;
	}

	public void addBackwardQuery(final BackwardQuery backwardQueryNode,
			EmptyStackWitnessListener<Statement, Val> listener) {
		backwardSolve(backwardQueryNode);
		for (ForwardQuery fw : backwardToForwardQueries.get(backwardQueryNode)) {
			queryToSolvers.getOrCreate(fw).synchedEmptyStackReachable(backwardQueryNode.asNode(), listener);
		}
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
		AbstractBoomerangSolver solver = queryToSolvers.getOrCreate(query);
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
		AbstractBoomerangSolver solver = queryToSolvers.getOrCreate(query);
		if (unit.isPresent()) {
			for (Unit succ : icfg().getSuccsOf(unit.get())) {
				Node<Statement, Val> source = new Node<Statement, Val>(
						new Statement((Stmt) succ, icfg().getMethodOf(succ)), query.asNode().fact());
				solver.solve(query.asNode(), source);
			}
		}
	}

	
	private class FieldWritePOI extends FieldStmtPOI {
		public FieldWritePOI(Statement statement, Val base, Field field, Val stored) {
			super(statement,base,field,stored);
		}

		@Override
		public void execute(ForwardQuery baseAllocation, Query flowAllocation) {
			if(flowAllocation instanceof BackwardQuery){
			} else if(flowAllocation instanceof ForwardQuery){
				executeImportAliases(baseAllocation, flowAllocation);
			}
		}
	}
	private class QueryWithVal{
		private final ForwardQuery q;
		private final Val v;

		QueryWithVal(ForwardQuery q, Val v){
			this.q = q;
			this.v = v;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((q == null) ? 0 : q.hashCode());
			result = prime * result + ((v == null) ? 0 : v.hashCode());
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
			if (q == null) {
				if (other.q != null)
					return false;
			} else if (!q.equals(other.q))
				return false;
			if (v == null) {
				if (other.v != null)
					return false;
			} else if (!v.equals(other.v))
				return false;
			return true;
		}

		private Boomerang getOuterType() {
			return Boomerang.this;
		}

		
	}
	
	private class ForwardCallSitePOI {
		private Statement callSite;
		private Statement returnSite;
		private Multimap<QueryWithVal,Query> flowSourceToBase = HashMultimap.create();
		private Multimap<ForwardQuery,Val> callSiteByPassingValues = HashMultimap.create();
		public ForwardCallSitePOI(Statement callSite, Statement returnSite){
			this.callSite = callSite;
			this.returnSite = returnSite;
		}

		public void addByPassingAllocation(ForwardQuery byPassingAllocation, Val value) {
			if(callSiteByPassingValues.put(byPassingAllocation,value)){
				for(Entry<QueryWithVal, Query> e : Lists.newArrayList(flowSourceToBase.entries())){
					if(e.getValue().equals(byPassingAllocation)){
						activateBase(e.getKey(), byPassingAllocation, value);
					}
				}
				
			}
		}

		public void addFlowAllocation(ForwardQuery flowSource, ForwardQuery aliasAlloc,  Val returnedVal) {
			QueryWithVal queryWithVal = new QueryWithVal(flowSource, returnedVal);
			if(flowSourceToBase.put(queryWithVal, aliasAlloc)){
				for(Val byPassing : Lists.newArrayList(callSiteByPassingValues.get(aliasAlloc))){
					activateBase(queryWithVal, aliasAlloc, byPassing);
				}
			}
		}

		private void activateBase(QueryWithVal queryWithVal, final ForwardQuery byPassingAllocation, final Val byPassing) {
			if(DISABLE_CALLPOI)
				return;
			final Val returnedVal = queryWithVal.v;
			final ForwardQuery flowSource = queryWithVal.q;
			if(byPassingAllocation.equals(flowSource)){
				return;
			}

			AbstractBoomerangSolver byPassingSolver = queryToSolvers.get(byPassingAllocation);
			WeightedPAutomaton<Field, INode<Node<Statement, Val>>, Weight<Field>> byPassingFieldAutomaton = byPassingSolver.getFieldAutomaton();
			Node<Statement,Val> source = new Node<Statement,Val>(returnSite,byPassing);
			final AbstractBoomerangSolver flowSolver = queryToSolvers.getOrCreate(flowSource);
			byPassingFieldAutomaton.registerListener(new ForwardDFSVisitor<Field, INode<Node<Statement,Val>>, Weight<Field>>(byPassingFieldAutomaton,new SingleNode<Node<Statement,Val>>(source), new ReachabilityListener<Field, INode<Node<Statement,Val>>>() {
				@Override
				public void reachable(Transition<Field, INode<Node<Statement, Val>>> transition) {
					flowSolver.getFieldAutomaton().addTransition(transition);
				}
			}));
			flowSolver.setFieldContextReachable(new Node<Statement,Val>(returnSite,byPassing));
			if(!byPassing.equals(returnedVal))
				flowSolver.addNormalCallFlow(new Node<Statement,Val>(returnSite,returnedVal),  new Node<Statement,Val>(returnSite,byPassing));
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
			result = prime * result + ((returnSite == null) ? 0 : returnSite.hashCode());
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
			if (returnSite == null) {
				if (other.returnSite != null)
					return false;
			} else if (!returnSite.equals(other.returnSite))
				return false;
			return true;
		}

		private Boomerang getOuterType() {
			return Boomerang.this;
		}
		
	}
	private class FieldReadPOI extends FieldStmtPOI {
		public FieldReadPOI(Statement statement, Val base, Field field, Val stored) {
			super(statement,base,field,stored);
		}
		
		@Override
		public void execute(final ForwardQuery baseAllocation, final Query flowAllocation) {
			if(flowAllocation instanceof ForwardQuery){
			} else if(flowAllocation instanceof BackwardQuery){
				executeImportAliases(baseAllocation, flowAllocation);
			}
		}
	}
	private abstract class FieldStmtPOI extends AbstractPOI<Statement, Val, Field> {
		private Multimap<Query,Val> aliases = HashMultimap.create();
		public FieldStmtPOI(Statement statement, Val base, Field field, Val storedVar) {
			super(statement, base, field, storedVar);
		}
		protected void executeImportAliases(final ForwardQuery baseAllocation, final Query flowAllocation){
			final AbstractBoomerangSolver baseSolver = queryToSolvers.get(baseAllocation);
			final AbstractBoomerangSolver flowSolver = queryToSolvers.get(flowAllocation);
			assert !flowSolver.getSuccsOf(getStmt()).isEmpty();
//			System.out.println(this.toString() + "     " + baseAllocation + "   " + flowAllocation);
			for(final Statement succOfWrite : flowSolver.getSuccsOf(getStmt())){
				baseSolver.getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, Weight<Field>>() {
	
					@Override
					public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
						final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
						if(aliasedVariableAtStmt.fact().stmt().equals(succOfWrite) && !(aliasedVariableAtStmt instanceof GeneratedState)){
							if(!aliases.put(flowAllocation, aliasedVariableAtStmt.fact().fact()))
								return;
							final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, Weight<Field>> aut = baseSolver.getFieldAutomaton();
							aut.registerListener(new ForwardDFSVisitor<Field, INode<Node<Statement,Val>>, Weight<Field>>(aut, aliasedVariableAtStmt, new ReachabilityListener<Field, INode<Node<Statement,Val>>>() {
							
								@Override
								public void reachable(Transition<Field, INode<Node<Statement,Val>>> transition) {
									if(!aliasedVariableAtStmt.fact().fact().equals(getBaseVar()) && !aliasedVariableAtStmt.fact().fact().equals(getStoredVar()))
										flowSolver.getFieldAutomaton().addTransition(transition);
								}
							}));
							flowSolver.getFieldAutomaton().addTransition(new Transition<Field, INode<Node<Statement,Val>>>(new AllocNode<Node<Statement,Val>>(baseAllocation.asNode()), Field.epsilon(), new SingleNode<Node<Statement,Val>>(new Node<Statement,Val>(succOfWrite,getBaseVar()))));
							if(!aliasedVariableAtStmt.fact().fact().equals(getBaseVar()) && !aliasedVariableAtStmt.fact().fact().equals(getStoredVar()))
								flowSolver.handlePOI(FieldStmtPOI.this, aliasedVariableAtStmt.fact());
						}
					}
	
					@Override
					public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, Weight<Field> w) {
					}
				});
			}
		}
	}
	
	public boolean addAllocationType(RefType type){
		if(allocatedTypes.add(type)){
			for(AbstractBoomerangSolver solvers : queryToSolvers.values()){
				solvers.addAllocatedType(type);
			}
			return true;
		}
		return false;
	}

	public void registerReachableMethodListener(ReachableMethodListener l){
		if(reachableMethodsListener.add(l)){
			for(AbstractBoomerangSolver s : queryToSolvers.values()){
				s.registerReachableMethodListener(l);
			}
		}
	}
	public abstract BiDiInterproceduralCFG<Unit, SootMethod> icfg();

	public Collection<? extends Node<Statement, Val>> getForwardReachableStates() {
		Set<Node<Statement, Val>> res = Sets.newHashSet();
		for (Query q : queryToSolvers.keySet()) {
			if (q instanceof ForwardQuery)
				res.addAll(queryToSolvers.getOrCreate(q).getReachedStates());
		}
		return res;
	}
	public DefaultValueMap<Query, AbstractBoomerangSolver> getSolvers() {
		return queryToSolvers;
	}
	
	public abstract Debugger createDebugger();
	
	public void debugOutput() {
		if(!DEBUG)
			return;
		Debugger debugger = createDebugger();
		for (Query q : queryToSolvers.keySet()) {
			debugger.reachableNodes(q,queryToSolvers.getOrCreate(q).getReachedStates());
			debugger.reachableCallNodes(q,queryToSolvers.getOrCreate(q).getReachedStates());
			debugger.reachableFieldNodes(q,queryToSolvers.getOrCreate(q).getReachedStates());
			
//			if (q instanceof ForwardQuery) {
				System.out.println("========================");
				System.out.println(q);
				System.out.println("========================");
				 queryToSolvers.getOrCreate(q).debugOutput();
				 for(FieldReadPOI  p : fieldReads.values()){
					 Stmt fieldReadStatement = p.getStmt().getUnit().get();
					 queryToSolvers.getOrCreate(q).debugFieldAutomaton(new Statement(fieldReadStatement,icfg().getMethodOf(fieldReadStatement)));
					 if(q instanceof ForwardQuery){
						 for(Unit succ : icfg().getSuccsOf(fieldReadStatement)){
							 queryToSolvers.getOrCreate(q).debugFieldAutomaton(new Statement((Stmt)succ,icfg().getMethodOf(fieldReadStatement)));
						 }
					 } else{
						 for(Unit succ : icfg().getPredsOf(p.getStmt().getUnit().get())){
							 queryToSolvers.getOrCreate(q).debugFieldAutomaton(new Statement((Stmt)succ,icfg().getMethodOf(fieldReadStatement)));
						 }
					 }
				 }
				 for(FieldWritePOI  p : fieldWrites.values()){
					 Stmt fieldWriteStatement = p.getStmt().getUnit().get();
					 queryToSolvers.getOrCreate(q).debugFieldAutomaton(new Statement(fieldWriteStatement,icfg().getMethodOf(fieldWriteStatement)));
					 if(q instanceof ForwardQuery){
						 for(Unit succ : icfg().getSuccsOf(fieldWriteStatement)){
							 queryToSolvers.getOrCreate(q).debugFieldAutomaton(new Statement((Stmt)succ,icfg().getMethodOf(fieldWriteStatement)));
						 }
					 } else{
						 for(Unit succ : icfg().getPredsOf(fieldWriteStatement)){
							 queryToSolvers.getOrCreate(q).debugFieldAutomaton(new Statement((Stmt)succ,icfg().getMethodOf(fieldWriteStatement)));
						 }
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
