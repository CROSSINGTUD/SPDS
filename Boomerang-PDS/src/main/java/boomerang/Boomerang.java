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
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.SyncPDSSolver;
import sync.pds.solver.SyncPDSSolver.StmtWithFact;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.WitnessNode.WitnessListener;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
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
					}
				}
			}

		});
		backwardSolver.registerListener(new SyncPDSUpdateListener<Statement, Value, Field>() {
			@Override
			public void onReachableNodeAdded(WitnessNode<Statement, Value, Field> node) {
				Optional<Stmt> optUnit = node.stmt().getUnit();
				System.out.println("BACKWARD SOLVER " + node);
				if(optUnit.isPresent()){
					Stmt stmt = optUnit.get();
					if(stmt instanceof AssignStmt){
						AssignStmt as = (AssignStmt) stmt;
						if(node.fact().equals(as.getLeftOp()) && as.getRightOp() instanceof NewExpr){
							addForwardQuery(new ForwardQuery(node.stmt(), as.getLeftOp()));
						}
					}
				}
			}
		});
	}
	protected void handleFieldWrite(WitnessNode<Statement, Value, Field> node, InstanceFieldRef ifr,
			AssignStmt as) {
		if(node.fact().equals(as.getRightOp())){
			addBackwardQuery(new BackwardQuery(node.stmt(), ifr.getBase()));
		}
		addAllocationSite(node, ifr,as);
	}
	private void addAllocationSite(final WitnessNode<Statement, Value, Field> node, final InstanceFieldRef ifr, final AssignStmt as) {
		if(node.fact().equals(ifr.getBase())){
			node.registerListener(new WitnessListener<Statement, Value, Field>() {
				@Override
				public void onAddCallWitnessTransition(Transition<Statement, INode<Value>> t) {
					System.out.println("CALL WITNESS " + node + "   " + t);
				}

				@Override
				public void onAddFieldWitnessTransition(
						Transition<Field, INode<SyncPDSSolver<Statement, Value, Field>.StmtWithFact>> t) {
					System.out.println("Field WITNESS " + node + "   " + t);
					if(!(t.getTarget() instanceof GeneratedState)){
						//otherwise we do have some fields on the stack
						SyncPDSSolver<Statement, Value, Field>.StmtWithFact target = t.getTarget().fact();
						Node<Statement, Value> alloc = new Node<Statement,Value>(target.stmt(),target.fact());
						if(activeAllocationSiteAtFieldWrite.put(as, target)){
							System.err.println("ADDEDINg ALLOC SITE " + alloc + as);
							Collection<Node<Statement, Value>> aliases = allAllocationSiteAtFieldWrite.get(new AllocAtStmt(alloc,as));
							for(Node<Statement, Value> alias : aliases){
								injectAlias( alias, as, new Field(ifr.getField()));
							}
						}
					}
				}
			});
		}

		node.registerListener(new WitnessListener<Statement, Value, Field>() {

			@Override
			public void onAddCallWitnessTransition(Transition<Statement, INode<Value>> t) {
			}

			@Override
			public void onAddFieldWitnessTransition(
					Transition<Field, INode<SyncPDSSolver<Statement, Value, Field>.StmtWithFact>> t) {
				if(!(t.getTarget() instanceof GeneratedState)){
					SyncPDSSolver<Statement, Value, Field>.StmtWithFact target = t.getTarget().fact();
					Node<Statement, Value> alloc = new Node<Statement,Value>(target.stmt(),target.fact());

					System.out.println("Field WITNESS ALIASED " + node + "   " + t);
					if(activeAllocationSiteAtFieldWrite.get(as).contains(target)){
						injectAlias(node.asNode(), as, new Field(ifr.getField()));
					} else{
						allAllocationSiteAtFieldWrite.put(new AllocAtStmt(alloc, as),node.asNode());
					}
				} else{
					System.out.println("NOT WITNESSS ALISAES FIELD " + t);
					System.out.println("asdasdINJECTION source" + node.asNode() + "\n \t at "+as + ifr);
					injectAlias2(t.getTarget(), as, t.getLabel());
				}
			}

			private void injectAlias2(INode<SyncPDSSolver<Statement, Value, Field>.StmtWithFact> alias, AssignStmt as,
					Field label) {
				System.out.println("INJECTION " + alias + as + ifr);
				for(Unit succ : icfg().getSuccsOf(as)){
					forwardSolver.injectAliasAtFieldWrite(alias, as,new Field(ifr.getField()), (Stmt) succ);
				}
			}

			
		});
	}
	private void injectAlias(Node<Statement, Value> alias, AssignStmt as, Field ifr) {
		System.out.println("INJECTION " + alias + as + ifr);
		for(Unit succ : icfg().getSuccsOf(as)){
			forwardSolver.injectAliasAtFieldWrite(alias, as, ifr, (Stmt) succ);
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
	protected void addBackwardQuery(BackwardQuery backwardQuery) {
		backwardSolve(backwardQuery);
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
				backwardSolver.solve(new Node<Statement,Value>(new Statement((Stmt) succ, icfg().getMethodOf(succ)), query.asNode().fact()));
			}
		}
//		backwardSolver.solve(query.asNode());
	}

	private void forwardSolve(ForwardQuery query) {
		Optional<Stmt> unit = query.asNode().stmt().getUnit();
		System.out.println("Solve " + query);
		if(unit.isPresent()){
			for(Unit succ : icfg().getSuccsOf(unit.get())){
				Node<Statement, Value> source = new Node<Statement,Value>(new Statement((Stmt) succ, icfg().getMethodOf(succ)), query.asNode().fact());
				forwardSolver.solve(source);
				addAllocationSite(source);
			}
		}
	}

	private void addAllocationSite(Node<Statement, Value> source) {
		if(allocationSite.containsKey(source))
			return;
		allocationSite.put(source,new AllocationSiteDFSVisitor(forwardSolver.getFieldAutomaton(),new SingleNode<SyncPDSSolver<Statement, Value, Field>.StmtWithFact>(forwardSolver.new StmtWithFact(source.stmt(),source.fact()))));
	}
	public abstract BiDiInterproceduralCFG<Unit, SootMethod> icfg();

	public Collection<? extends Node<Statement, Value>> getForwardReachableStates() {
		return forwardSolver.getReachedStates();
	}
	public class AllocationSiteDFSVisitor extends ForwardDFSVisitor<Field,  INode<SyncPDSSolver<Statement, Value, Field>.StmtWithFact>, Weight<Field>>{

		public AllocationSiteDFSVisitor(WeightedPAutomaton<Field, INode<SyncPDSSolver<Statement, Value, Field>.StmtWithFact>, Weight<Field>> aut,
				INode<SyncPDSSolver<Statement, Value, Field>.StmtWithFact> startState) {
			super(aut, startState);
		}

	}
	
}
