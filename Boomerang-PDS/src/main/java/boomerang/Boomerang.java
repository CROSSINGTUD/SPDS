package boomerang;

import java.util.Collection;
import java.util.Map;

import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import pathexpression.IRegEx;
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
import sync.pds.solver.SyncPDSSolver;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WitnessListener;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import sync.pds.weights.SetDomain;
import wpds.impl.PushRule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.ForwardDFSVisitor;
import wpds.interfaces.WPAUpdateListener;

public abstract class Boomerang {
	private final ForwardBoomerangSolver forwardSolver = new ForwardBoomerangSolver(icfg());
	private final BackwardBoomerangSolver backwardSolver = new BackwardBoomerangSolver(bwicfg());
	private BackwardsInterproceduralCFG bwicfg;
	private Multimap<AllocAtStmt, Node<Statement,Value>> allAllocationSiteAtFieldWrite = HashMultimap.create(); 
	private Multimap<Stmt,Node<Statement,Value>> activeAllocationSiteAtFieldWrite = HashMultimap.create();
	private Multimap<AllocAtStmt, Node<Statement,Value>> allAllocationSiteAtFieldRead = HashMultimap.create(); 
	private Multimap<Stmt,Node<Statement,Value>> activeAllocationSiteAtFieldRead = HashMultimap.create();
	private Map<Node<Statement,Value>, AllocationSiteDFSVisitor> allocationSite = Maps.newHashMap();
	private Collection<ForwardQuery> forwardQueries = Sets.newHashSet();
	private Collection<BackwardQuery> backwardQueries = Sets.newHashSet();
	
	public Boomerang(){
		forwardSolver.registerListener(new SyncPDSUpdateListener<Statement, Value, Field>() {
			@Override
			public void onReachableNodeAdded(WitnessNode<Statement, Value, Field> node) {
				Optional<Stmt> optUnit = node.stmt().getUnit();
				if(optUnit.isPresent()){
					Stmt stmt = optUnit.get();
					if(stmt instanceof AssignStmt){
						AssignStmt as = (AssignStmt) stmt;
						if(as.getLeftOp() instanceof InstanceFieldRef){
							InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
							handleFieldWrite(node,ifr,as);
						}
						if(as.getRightOp() instanceof InstanceFieldRef){
							InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
							attachHandlerFieldRead(node, ifr, as);
						}
					}
				}
			}
		});
		backwardSolver.registerListener(new SyncPDSUpdateListener<Statement, Value, Field>() {
			@Override
			public void onReachableNodeAdded(WitnessNode<Statement, Value, Field> node) {
				Optional<Stmt> optUnit = node.stmt().getUnit();
				if(optUnit.isPresent()){
					Stmt stmt = optUnit.get();
					if(stmt instanceof AssignStmt){
						AssignStmt as = (AssignStmt) stmt;
						if(node.fact().equals(as.getLeftOp()) && isAllocationValue(as.getRightOp())){
							addForwardQuery(new ForwardQuery(node.stmt(), as.getLeftOp()));
						}
						
						if(as.getRightOp() instanceof InstanceFieldRef){
							InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
							handleFieldRead(node,ifr,as);
						}
					}
				}
			}
		});
	}
	protected void handleFieldRead(WitnessNode<Statement, Value, Field> node, final InstanceFieldRef ifr, final AssignStmt as) {
		if(node.fact().equals(as.getLeftOp())){
			BackwardQuery backwardQuery = new BackwardQuery(node.stmt(), ifr.getBase());
			addBackwardQuery(backwardQuery, new WitnessListener<Statement, Value>() {
				@Override
				public void witnessFound(Node<Statement, Value> allocation) {
					if(activeAllocationSiteAtFieldRead.put(as, allocation)){
						Collection<Node<Statement, Value>> aliases = allAllocationSiteAtFieldRead.removeAll(new AllocAtStmt(allocation,as));
						for(Node<Statement, Value> alias : aliases){
							injectBackwardAlias(alias, as, ifr.getBase(), new Field(ifr.getField()));
						}
					}
				}
			});
		}
	}
	public static boolean isAllocationValue(Value val) {
		return val instanceof NullConstant || val instanceof NewExpr;
	}
	protected void handleFieldWrite(WitnessNode<Statement, Value, Field> node, final InstanceFieldRef ifr,
			final AssignStmt as) {
		if(node.fact().equals(as.getRightOp())){
			BackwardQuery backwardQuery = new BackwardQuery(node.stmt(), ifr.getBase());
			addBackwardQuery(backwardQuery,new WitnessListener<Statement, Value>() {
				@Override
				public void witnessFound(Node<Statement, Value> alloc) {
					if(activeAllocationSiteAtFieldWrite.put(as, alloc)){
						Collection<Node<Statement, Value>> aliases = allAllocationSiteAtFieldWrite.removeAll(new AllocAtStmt(alloc,as));
						for(Node<Statement, Value> alias : aliases){
							injectForwardAlias(alias, as,ifr.getBase(),  new Field(ifr.getField()));
						}
					}
				}
			});
		}
		addAllocationSite(node, ifr,as);
	}
	private void addAllocationSite(final WitnessNode<Statement, Value, Field> node, final InstanceFieldRef ifr, final AssignStmt as) {
		forwardSolver.synchedEmptyStackReachable(node.asNode(), new WitnessListener<Statement, Value>() {

			@Override
			public void witnessFound(Node<Statement, Value> alloc) {
				if(activeAllocationSiteAtFieldWrite.get(as).contains(alloc)){
					injectForwardAlias(node.asNode(), as, ifr.getBase(), new Field(ifr.getField()));
				} else{
					allAllocationSiteAtFieldWrite.put(new AllocAtStmt(alloc, as),node.asNode());
				}
			}
		});
	}
	private void injectAliasWithStack(INode<Node<Statement,Value>> alias, AssignStmt as,
			Field label, InstanceFieldRef ifr) {
//		System.out.println("INJECTION " + alias + as + ifr);
		for(Unit succ : icfg().getSuccsOf(as)){	
			//TODO Why don't we need succ here?
			Node<Statement,Value> sourceNode = new Node<Statement,Value>(new Statement(as, icfg().getMethodOf(as)),  as.getRightOp());
			SetDomain<Field, Statement, Value> one = SetDomain.<Field,Statement,Value>one();
			INode<Node<Statement,Value>> source = new SingleNode<Node<Statement,Value>>(sourceNode);
			forwardSolver.injectFieldRule(new PushRule<Field, INode<Node<Statement,Value>>, Weight<Field>>(source, Field.wildcard(), alias,  new Field(ifr.getField()), Field.wildcard(),one));
		}
	}
	private void attachHandlerFieldRead(final WitnessNode<Statement, Value, Field> node, final InstanceFieldRef ifr, final AssignStmt fieldRead) {
		forwardSolver.addFieldAutomatonListener(new WPAUpdateListener<Field, INode<Node<Statement,Value>>, Weight<Field>>() {

			@Override
			public void onAddedTransition(Transition<Field, INode<Node<Statement, Value>>> t) {
				if(t.getStart() instanceof GeneratedState)
					return;
				if(!t.getStart().fact().equals(node.asNode()))
					return;
				if(!(t.getTarget() instanceof GeneratedState)){
					Node<Statement,Value> target = t.getTarget().fact();
					Node<Statement, Value> alloc = new Node<Statement,Value>(target.stmt(),target.fact());

					if(activeAllocationSiteAtFieldRead.get(fieldRead).contains(target)){
						injectBackwardAlias(node.asNode(), fieldRead, ifr.getBase(), new Field(ifr.getField()));
					} else{
						System.out.println("Queuing alloc: " + alloc + fieldRead);
						allAllocationSiteAtFieldRead.put(new AllocAtStmt(alloc, fieldRead),node.asNode());
					}
				} else{
					//TODO only do so, if we have an alias
//					System.out.println("NOT WITNESSS ALISAES FIELD " + t);
//					System.out.println("asdasdINJECTION source" + node.asNode() + "\n \t at "+as + ifr);
				}
			}

			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Value>>> t, Weight<Field> w) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	private void injectForwardAlias(Node<Statement, Value> alias, AssignStmt as, Value base, Field ifr) {
		if(alias.fact().equals(base))
			return;
		System.out.println("Injecting forward alias " + alias + ifr);
		for(Unit succ : icfg().getSuccsOf(as)){
			Node<Statement,Value> sourceNode = new Node<Statement,Value>(new Statement(as, icfg().getMethodOf(as)),  as.getRightOp());
			Node<Statement,Value>  targetNode = new Node<Statement,Value>(new Statement((Stmt) succ, icfg().getMethodOf(succ)), alias.fact());
			forwardSolver.injectFieldRule(sourceNode, ifr, targetNode);
		}
	}
	
	private void injectBackwardAlias(Node<Statement, Value> alias, AssignStmt as, Value base, Field ifr) {
		if(alias.fact().equals(base))
			return;
		System.out.println("Injecting backward alias " + alias + ifr);
		for(Unit succ : bwicfg().getSuccsOf(as)){
			Node<Statement,Value> source = new Node<Statement,Value>(new Statement(as, icfg().getMethodOf(as)),  as.getLeftOp());
			Node<Statement,Value>  target = new Node<Statement,Value>(new Statement((Stmt) succ, icfg().getMethodOf(succ)), alias.fact());
			backwardSolver.injectFieldRule(source,  ifr,target);
		}
	}
	private BiDiInterproceduralCFG<Unit, SootMethod> bwicfg() {
		if(bwicfg == null)
			bwicfg = new BackwardsInterproceduralCFG(icfg());
		return bwicfg;
	}
	protected void addForwardQuery(ForwardQuery query) {
		forwardQueries.add(query);
		forwardSolver.getFieldAutomaton().addFinalState(new SingleNode<Node<Statement,Value>>(query.asNode()));
		forwardSolve(query);
	}
	public void addBackwardQuery(final BackwardQuery backwardQueryNode, WitnessListener<Statement, Value> listener) {
		backwardSolve(backwardQueryNode);
		backwardQueries.add(backwardQueryNode);
		forwardSolver.synchedEmptyStackReachable(backwardQueryNode.asNode(), listener);
	}
	protected void addListeners(final BackwardQuery backwardQueryNode,
			final WPAUpdateListener<Field, INode<Node<Statement, Value>>, Weight<Field>> fieldListener,
			final WPAUpdateListener<Statement, INode<Value>, Weight<Statement>> callListener) {
		forwardSolver.addFieldAutomatonListener(new WPAUpdateListener<Field, INode<Node<Statement,Value>>, Weight<Field>>() {

			@Override
			public void onAddedTransition(Transition<Field, INode<Node<Statement, Value>>> t) {
				if(t.getStart() instanceof GeneratedState)
					return;
				if(t.getStart().fact().equals(backwardQueryNode.asNode())){
					fieldListener.onAddedTransition(t);
				}
			}

			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Value>>> t, Weight<Field> w) {
			}
		});
		if(callListener != null)
			forwardSolver.addCallAutomatonListener(new WPAUpdateListener<Statement, INode<Value>, Weight<Statement>>() {
	
				@Override
				public void onAddedTransition(Transition<Statement, INode<Value>> t) {
					if(t.getStart() instanceof GeneratedState)
						return;
					if(t.getLabel().equals(backwardQueryNode.asNode().stmt()) && backwardQueryNode.asNode().fact().equals(t.getStart().fact())){
						callListener.onAddedTransition(t);
					}
				}
	
				@Override
				public void onWeightAdded(Transition<Statement, INode<Value>> t, Weight<Statement> w) {
				}
			});
	}
	public void solve(Query query) {
		if (query instanceof ForwardQuery) {
			forwardSolve((ForwardQuery) query);
		}
		if (query instanceof BackwardQuery) {
			backwardSolve((BackwardQuery) query);
		}
	}

	private void backwardSolve(BackwardQuery query) {
		System.out.println("Backward solving query: " + query);
		Optional<Stmt> unit = query.asNode().stmt().getUnit();
		if(unit.isPresent()){
			for(Unit succ : new BackwardsInterproceduralCFG(icfg()).getSuccsOf(unit.get())){
				backwardSolver.solve(query.asNode(), new Node<Statement,Value>(new Statement((Stmt) succ, icfg().getMethodOf(succ)), query.asNode().fact()));
			}
		}
	}

	private void forwardSolve(ForwardQuery query) {
		Optional<Stmt> unit = query.asNode().stmt().getUnit();
		System.out.println("Forward solving query: " + query);
		if(unit.isPresent()){
			for(Unit succ : icfg().getSuccsOf(unit.get())){
				Node<Statement, Value> source = new Node<Statement,Value>(new Statement((Stmt) succ, icfg().getMethodOf(succ)), query.asNode().fact());
				forwardSolver.solve(query.asNode(), source);
				addAllocationSite(source);
			}
		}
	}

	private void addAllocationSite(Node<Statement, Value> source) {
		if(allocationSite.containsKey(source))
			return;
		allocationSite.put(source,new AllocationSiteDFSVisitor(forwardSolver.getFieldAutomaton(),new SingleNode<Node<Statement,Value>>(new Node<Statement,Value>(source.stmt(),source.fact()))));
	}
	public abstract BiDiInterproceduralCFG<Unit, SootMethod> icfg();

	public Collection<? extends Node<Statement, Value>> getForwardReachableStates() {
		return forwardSolver.getReachedStates();
	}
	public class AllocationSiteDFSVisitor extends ForwardDFSVisitor<Field,  INode<Node<Statement,Value>>, Weight<Field>>{

		public AllocationSiteDFSVisitor(WeightedPAutomaton<Field, INode<Node<Statement,Value>>, Weight<Field>> aut,
				INode<Node<Statement,Value>> startState) {
			super(aut, startState);
		}
	}
	
	public void debugOutput(){
		backwardSolver.debugOutput();
		forwardSolver.debugOutput();
		for(ForwardQuery fq : forwardQueries){
			for(Node<Statement, Value> bq : forwardSolver.getReachedStates()){
				IRegEx<Field> extractLanguage = forwardSolver.getFieldAutomaton().extractLanguage(new SingleNode<Node<Statement,Value>>(bq),new SingleNode<Node<Statement,Value>>(fq.asNode()));
				System.out.println(bq + " "+ fq +" "+ extractLanguage);
			}
		}
		for(final BackwardQuery bq : backwardQueries){
			forwardSolver.synchedEmptyStackReachable(bq.asNode(), new WitnessListener<Statement, Value>() {
				
				@Override
				public void witnessFound(Node<Statement, Value> targetFact) {
					System.out.println(bq + " is allocated at " +targetFact);
				}
			});
		}

//		for(ForwardQuery fq : forwardQueries){
//			System.out.println(fq);
//			System.out.println(Joiner.on("\n\t").join(forwardSolver.getFieldAutomaton().dfs(new SingleNode<Node<Statement,Value>>(fq.asNode()))));
//		}
	}
}
