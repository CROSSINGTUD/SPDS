package boomerang;

import java.util.Collection;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.poi.AbstractPOI;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import heros.utilities.DefaultValueMap;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.EmptyStackWitnessListener;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.ForwardDFSVisitor;
import wpds.interfaces.ReachabilityListener;
import wpds.interfaces.WPAUpdateListener;

public abstract class Boomerang {
	private final DefaultValueMap<Query, AbstractBoomerangSolver> queryToSolvers = new DefaultValueMap<Query, AbstractBoomerangSolver>() {
		@Override
		protected AbstractBoomerangSolver createItem(Query key) {
			if (key instanceof BackwardQuery)
				return createBackwardSolver((BackwardQuery) key);
			else
				return createForwardSolver((ForwardQuery) key);
		}
	};
	private BackwardsInterproceduralCFG bwicfg;
	private Collection<ForwardQuery> forwardQueries = Sets.newHashSet();
	private Collection<BackwardQuery> backwardQueries = Sets.newHashSet();
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
	protected AbstractBoomerangSolver createBackwardSolver(final BackwardQuery backwardQuery) {
		BackwardBoomerangSolver solver = new BackwardBoomerangSolver(bwicfg(), backwardQuery);
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
										queryToSolvers.getOrCreate(forwardQuery).addUnbalancedFlow(generatedState.location());
									}
								}

								@Override
								public void onWeightAdded(Transition<Statement, INode<Val>> t, Weight<Statement> w) {
									
								}
							});
						}

						if (as.getRightOp() instanceof InstanceFieldRef) {
							InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
							handleFieldRead(node, ifr, as, backwardQuery);
						}
//						if(as.getLeftOp() instanceof InstanceFieldRef){
//							InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
//							handleFieldWriteBackward(node, ifr, as, backwardQuery);
//						}
					}
				}
			}

		});
		return solver;
	}

	protected AbstractBoomerangSolver createForwardSolver(final ForwardQuery sourceQuery) {
		ForwardBoomerangSolver solver = new ForwardBoomerangSolver(icfg(), sourceQuery);
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
							handleFieldWrite(node, ifr, as, sourceQuery);
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

	protected void handleFieldRead(final WitnessNode<Statement, Val, Field> node, final InstanceFieldRef ifr,
			final AssignStmt as, final BackwardQuery sourceQuery) {	
		Val base = new Val(ifr.getBase(), icfg().getMethodOf(as));
		BackwardQuery backwardQuery = new BackwardQuery(node.stmt(),
					base);
		Field field = new Field(ifr.getField());
		if (node.fact().value().equals(as.getLeftOp())) {
			addBackwardQuery(backwardQuery, new EmptyStackWitnessListener<Statement, Val>() {
				@Override
				public void witnessFound(Node<Statement, Val> alloc) {
				}
			});
			fieldReads.getOrCreate(new FieldReadPOI(node.stmt(), base, field, new Val(as.getLeftOp(),icfg().getMethodOf(as)))).addFlowAllocation(sourceQuery);
		}
	}

	public static boolean isAllocationVal(Value val) {
		return val instanceof NullConstant || val instanceof NewExpr;
	}

	protected void handleFieldWrite(final WitnessNode<Statement, Val, Field> node, final InstanceFieldRef ifr,
			final AssignStmt as, final ForwardQuery sourceQuery) {
		Val base = new Val(ifr.getBase(), icfg().getMethodOf(as));
		BackwardQuery backwardQuery = new BackwardQuery(node.stmt(),
				base);
		final Field field = new Field(ifr.getField());
		if (node.fact().value().equals(as.getRightOp())) {
			addBackwardQuery(backwardQuery, new EmptyStackWitnessListener<Statement, Val>() {
				@Override
				public void witnessFound(Node<Statement, Val> alloc) {
				}
			});
			fieldWrites.getOrCreate(new FieldWritePOI(node.stmt(),base, field, new Val(as.getRightOp(),icfg().getMethodOf(as)), as, base)).addFlowAllocation(sourceQuery);
		}
		if (node.fact().value().equals(ifr.getBase())) {
			fieldWrites.getOrCreate(new FieldWritePOI(node.stmt(),base, field, new Val(as.getRightOp(),icfg().getMethodOf(as)), as, base)).addBaseAllocation(sourceQuery);
		}
	}


	private void attachHandlerFieldRead(final WitnessNode<Statement, Val, Field> node, final InstanceFieldRef ifr,
			final AssignStmt fieldRead, final ForwardQuery sourceQuery) {
		if (node.fact().value().equals(ifr.getBase())) {
			Val base = new Val(ifr.getBase(), icfg().getMethodOf(fieldRead));
			Field field = new Field(ifr.getField());
			fieldReads.getOrCreate(new FieldReadPOI(node.stmt(), base,field, new Val(fieldRead.getLeftOp(), icfg().getMethodOf(fieldRead)))).addBaseAllocation(sourceQuery);
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
		System.out.println("Backward solving query: " + query);
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
		System.out.println("Forward solving query: " + query);
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

	private class FieldWritePOI extends AbstractPOI<Statement, Val, Field> {
		public FieldWritePOI(Statement statement, Val leftOp, Field field, Val rightOp, AssignStmt fieldWriteStatement, Val base) {
			super(statement, leftOp, field, rightOp);
		}

		@Override
		public void execute(final Query baseAllocation, final Query flowAllocation) {
			for(final Statement succOfWrite : getSuccsOf(getStmt())){
			queryToSolvers.get(baseAllocation).getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, Weight<Field>>() {

				@Override
				public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
					final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
					if(aliasedVariableAtStmt.fact().stmt().equals(succOfWrite) && !(aliasedVariableAtStmt instanceof GeneratedState)){
						if(aliasedVariableAtStmt.fact().fact().equals(getBaseVar()) || aliasedVariableAtStmt.fact().fact().equals(getStoredVar()))
							return;
						final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, Weight<Field>> aut = queryToSolvers.getOrCreate(baseAllocation).getFieldAutomaton();
						final AbstractBoomerangSolver currentSolver = queryToSolvers.getOrCreate(flowAllocation);
						aut.registerListener(new ForwardDFSVisitor<Field, INode<Node<Statement,Val>>, Weight<Field>>(aut, aliasedVariableAtStmt, new ReachabilityListener<Field, INode<Node<Statement,Val>>>() {
						
							@Override
							public void reachable(Transition<Field, INode<Node<Statement,Val>>> transition) {
								
								if(transition.getStart() instanceof GeneratedState){
									currentSolver.addGeneratedFieldState((GeneratedState<Node<Statement,Val>, Field>)transition.getStart());
								}
								if(transition.getTarget() instanceof GeneratedState){
									currentSolver.addGeneratedFieldState((GeneratedState<Node<Statement,Val>, Field>)transition.getTarget());
								}
								currentSolver.getFieldAutomaton().addTransition(transition);	
								
								if(transition.getTarget().fact().equals(baseAllocation.asNode())){
									currentSolver.connectBase(FieldWritePOI.this, transition.getStart(), succOfWrite);
								}
							}
						}));
						currentSolver.handlePOI(FieldWritePOI.this, aliasedVariableAtStmt.fact());
					}
				}

				@Override
				public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, Weight<Field> w) {
				}
			});
			}
		}
	}
	
	
	private class FieldReadPOI extends AbstractPOI<Statement, Val, Field> {

		public FieldReadPOI(Statement statement, Val base, Field field, Val stored) {
			super(statement,base,field,stored);
		}

		@Override
		public void execute(final Query baseAllocation, final Query flowAllocation) {
			queryToSolvers.get(baseAllocation).getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, Weight<Field>>() {

				@Override
				public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
					INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
					if(aliasedVariableAtStmt.fact().stmt().equals(getStmt()) && !(t.getStart() instanceof GeneratedState)){
						final AbstractBoomerangSolver currentSolver = queryToSolvers.getOrCreate(flowAllocation);
						WeightedPAutomaton<Field, INode<Node<Statement, Val>>, Weight<Field>> aut = queryToSolvers.getOrCreate(baseAllocation).getFieldAutomaton();
						aut.registerListener(new ForwardDFSVisitor<Field, INode<Node<Statement,Val>>, Weight<Field>>(aut, t.getStart(), new ReachabilityListener<Field, INode<Node<Statement,Val>>>() {
							@Override
							public void reachable(Transition<Field, INode<Node<Statement,Val>>> transition) {
								currentSolver.getFieldAutomaton().addTransition(transition);	
								if(transition.getTarget().fact().equals(baseAllocation.asNode())){
//									currentSolver.connectBase(FieldReadPOI.this, transition.getStart().fact());
							 	}
							}
						}));
						System.err.println("BACKWARd " + aliasedVariableAtStmt);
						currentSolver.handlePOI(FieldReadPOI.this, aliasedVariableAtStmt.fact());	
					}
				}

				@Override
				public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, Weight<Field> w) {
				}
			});
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
	
	private Set<Statement> getSuccsOf(Statement stmt) {
		Set<Statement> res = Sets.newHashSet();
		if(!stmt.getUnit().isPresent())
			return res;
		Stmt curr = stmt.getUnit().get();
		for(Unit succ : icfg().getSuccsOf(curr)){
			res.add(new Statement((Stmt) succ, icfg().getMethodOf(succ)));
		}
		return res;
	}
	
	public void debugOutput() {
		for (Query q : queryToSolvers.keySet()) {
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
