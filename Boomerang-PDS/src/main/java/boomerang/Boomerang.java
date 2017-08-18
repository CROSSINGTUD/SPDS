package boomerang;

import java.util.Collection;
import java.util.Map;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
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
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.WitnessNode.WitnessListener;
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

public abstract class Boomerang {
	private final ForwardBoomerangSolver forwardSolver = new ForwardBoomerangSolver(icfg());
	private final BackwardBoomerangSolver backwardSolver = new BackwardBoomerangSolver(bwicfg());
	private BackwardsInterproceduralCFG bwicfg;
	private Multimap<AllocAtStmt, Node<Statement,Value>> allAllocationSiteAtFieldWrite = HashMultimap.create(); 
	private Multimap<Stmt,Node<Statement,Value>> activeAllocationSiteAtFieldWrite = HashMultimap.create();
	private Multimap<AllocAtStmt, Node<Statement,Value>> allAllocationSiteAtFieldRead = HashMultimap.create(); 
	private Multimap<Stmt,Node<Statement,Value>> activeAllocationSiteAtFieldRead = HashMultimap.create();
	private Map<Node<Statement,Value>, AllocationSiteDFSVisitor> allocationSite = Maps.newHashMap();
	
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
			addBackwardQuery(new BackwardQuery(node.stmt(), ifr.getBase()), new WitnessListener<Statement, Value, Field>() {
				@Override
				public void onAddCallWitnessTransition(Transition<Statement, INode<Value>> t) {
				}

				@Override
				public void onAddFieldWitnessTransition(
						Transition<Field, INode<Node<Statement,Value>>> t) {
					if(!(t.getTarget() instanceof GeneratedState)){
						//otherwise we do have some fields on the stack and do not care
						Node<Statement,Value> target = t.getTarget().fact();
						Node<Statement, Value> alloc = new Node<Statement,Value>(target.stmt(),target.fact());
						if(activeAllocationSiteAtFieldRead.put(as, target)){
							System.out.println("ACTIVATING  " + as + " " + target);
							System.out.println("GET   " + new AllocAtStmt(alloc,as));
							Collection<Node<Statement, Value>> aliases = allAllocationSiteAtFieldRead.get(new AllocAtStmt(alloc,as));
							System.out.println("BACKWARD INJECTION OF " + aliases);
							for(Node<Statement, Value> alias : aliases){
								injectBackwardAlias(alias, as, new Field(ifr.getField()));
							}
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
			addBackwardQuery(new BackwardQuery(node.stmt(), ifr.getBase()), new WitnessListener<Statement, Value, Field>() {
				@Override
				public void onAddCallWitnessTransition(Transition<Statement, INode<Value>> t) {
				}

				@Override
				public void onAddFieldWitnessTransition(
						Transition<Field, INode<Node<Statement,Value>>> t) {
					if(!(t.getTarget() instanceof GeneratedState)){
						//otherwise we do have some fields on the stack and do not care
						Node<Statement,Value> target = t.getTarget().fact();
						Node<Statement, Value> alloc = new Node<Statement,Value>(target.stmt(),target.fact());
						if(activeAllocationSiteAtFieldWrite.put(as, target)){
							Collection<Node<Statement, Value>> aliases = allAllocationSiteAtFieldWrite.get(new AllocAtStmt(alloc,as));
							for(Node<Statement, Value> alias : aliases){
								injectForwardAlias(alias, as, new Field(ifr.getField()));
							}
						}
					}
				}
			});
		}
		addAllocationSite(node, ifr,as);
	}
	private void addAllocationSite(final WitnessNode<Statement, Value, Field> node, final InstanceFieldRef ifr, final AssignStmt as) {
		node.registerListener(new WitnessListener<Statement, Value, Field>() {

			@Override
			public void onAddCallWitnessTransition(Transition<Statement, INode<Value>> t) {
			}

			@Override
			public void onAddFieldWitnessTransition(
					Transition<Field, INode<Node<Statement,Value>>> t) {
				if(!(t.getTarget() instanceof GeneratedState)){
					Node<Statement,Value> target = t.getTarget().fact();
					Node<Statement, Value> alloc = new Node<Statement,Value>(target.stmt(),target.fact());

					System.out.println("Field WITNESS ALIASED " + node + "   " + t);
					if(activeAllocationSiteAtFieldWrite.get(as).contains(target)){
						injectForwardAlias(node.asNode(), as, new Field(ifr.getField()));
					} else{
						allAllocationSiteAtFieldWrite.put(new AllocAtStmt(alloc, as),node.asNode());
					}
				} else{
					//TODO only do so, if we have an alias
					System.out.println("NOT WITNESSS ALISAES FIELD " + t);
					System.out.println("asdasdINJECTION source" + node.asNode() + "\n \t at "+as + ifr);
					injectAliasWithStack(t.getTarget(), as, t.getLabel());
				}
			}

			private void injectAliasWithStack(INode<Node<Statement,Value>> alias, AssignStmt as,
					Field label) {
				System.out.println("INJECTION " + alias + as + ifr);
				for(Unit succ : icfg().getSuccsOf(as)){	
					//TODO Why don't we need succ here?
					Node<Statement,Value> sourceNode = new Node<Statement,Value>(new Statement(as, icfg().getMethodOf(as)),  as.getRightOp());
					SetDomain<Field, Statement, Value> one = SetDomain.<Field,Statement,Value>one();
					INode<Node<Statement,Value>> source = new SingleNode<Node<Statement,Value>>(sourceNode);
					forwardSolver.injectFieldRule(new PushRule<Field, INode<Node<Statement,Value>>, Weight<Field>>(source, Field.wildcard(), alias,  new Field(ifr.getField()), Field.wildcard(),one));
				}
			}
		});
	}
	
	private void attachHandlerFieldRead(final WitnessNode<Statement, Value, Field> node, final InstanceFieldRef ifr, final AssignStmt fieldRead) {
		node.registerListener(new WitnessListener<Statement, Value, Field>() {

			@Override
			public void onAddCallWitnessTransition(Transition<Statement, INode<Value>> t) {
			}

			@Override
			public void onAddFieldWitnessTransition(
					Transition<Field, INode<Node<Statement,Value>>> t) {
				if(!(t.getTarget() instanceof GeneratedState)){
					Node<Statement,Value> target = t.getTarget().fact();
					Node<Statement, Value> alloc = new Node<Statement,Value>(target.stmt(),target.fact());

					System.out.println("Field Backward WITNESS ALIASED " + node + "   " + t);
					if(activeAllocationSiteAtFieldRead.get(fieldRead).contains(target)){
						System.out.println("Exec");
						injectBackwardAlias(node.asNode(), fieldRead, new Field(ifr.getField()));
					} else{
						System.out.println("Queue");
						System.out.println("Q  " + new AllocAtStmt(alloc, fieldRead));
						allAllocationSiteAtFieldRead.put(new AllocAtStmt(alloc, fieldRead),node.asNode());
					}
				} else{
					//TODO only do so, if we have an alias
//					System.out.println("NOT WITNESSS ALISAES FIELD " + t);
//					System.out.println("asdasdINJECTION source" + node.asNode() + "\n \t at "+as + ifr);
				}
			}
		});
	}
	private void injectForwardAlias(Node<Statement, Value> alias, AssignStmt as, Field ifr) {
		System.out.println("INJECTION " + alias + as + ifr);
		for(Unit succ : icfg().getSuccsOf(as)){
			Node<Statement,Value> sourceNode = new Node<Statement,Value>(new Statement(as, icfg().getMethodOf(as)),  as.getRightOp());
			Node<Statement,Value>  targetNode = new Node<Statement,Value>(new Statement((Stmt) succ, icfg().getMethodOf(succ)), alias.fact());
			forwardSolver.injectFieldRule(sourceNode, ifr, targetNode);
		}
	}
	
	private void injectBackwardAlias(Node<Statement, Value> alias, AssignStmt as, Field ifr) {
		System.out.println("BAckward INJECTION " + alias + as + ifr);
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
		forwardSolve(query);
	}
	public void addBackwardQuery(final BackwardQuery backwardQueryNode, final WitnessListener<Statement, Value, Field> listener) {
		backwardSolve(backwardQueryNode);
		forwardSolver.registerListener(new SyncPDSUpdateListener<Statement, Value, Field>() {

			@Override
			public void onReachableNodeAdded(WitnessNode<Statement, Value, Field> node) {
				if(node.asNode().equals(backwardQueryNode.asNode())){
					node.registerListener(listener);
				}
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
		System.out.println("Solve " + query);
		Optional<Stmt> unit = query.asNode().stmt().getUnit();
		if(unit.isPresent()){
			for(Unit succ : new BackwardsInterproceduralCFG(icfg()).getSuccsOf(unit.get())){
				backwardSolver.solve(query.asNode(), new Node<Statement,Value>(new Statement((Stmt) succ, icfg().getMethodOf(succ)), query.asNode().fact()));
			}
		}
	}

	private void forwardSolve(ForwardQuery query) {
		Optional<Stmt> unit = query.asNode().stmt().getUnit();
		System.out.println("Solve " + query);
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
	
}
