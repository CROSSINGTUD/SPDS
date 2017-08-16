package boomerang;

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

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
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.nodes.Node;

public abstract class Boomerang {
	private final ForwardBoomerangSolver forwardSolver = new ForwardBoomerangSolver(icfg());
	private final BackwardBoomerangSolver backwardSolver = new BackwardBoomerangSolver(bwicfg());
	private BackwardsInterproceduralCFG bwicfg;
	private Multimap<Node<Statement,Value>,Node<Statement,Value>> allAllocationSiteAtFieldWrite = HashMultimap.create(); 
	private Multimap<Stmt,Node<Statement,Value>> activeAllocationSiteAtFieldWrite = HashMultimap.create();
	
	public Boomerang(){
		forwardSolver.registerListener(new SyncPDSUpdateListener<Statement, Value, Field>() {
			@Override
			public void onReachableNodeAdded(SyncPDSSolver<Statement, Value, Field>.QueuedNode node) {
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
			public void onReachableNodeAdded(SyncPDSSolver<Statement, Value, Field>.QueuedNode node) {
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
	protected void handleFieldWrite(SyncPDSSolver<Statement, Value, Field>.QueuedNode node, InstanceFieldRef ifr,
			AssignStmt as) {
		if(node.fact().equals(as.getRightOp())){
			addBackwardQuery(new BackwardQuery(node.stmt(), ifr.getBase()), ifr.getField(), node);
		}
		addAllocationSite(node.source(),node.asNode(), ifr,as);
	}
	private void addAllocationSite(Node<Statement, Value> alloc, Node<Statement, Value> target, InstanceFieldRef ifr, AssignStmt as) {
		if(target.fact().equals(ifr.getBase())){
			if(activeAllocationSiteAtFieldWrite.put(as, alloc)){
				Collection<Node<Statement, Value>> aliases = allAllocationSiteAtFieldWrite.removeAll(alloc);
				for(Node<Statement, Value> alias : aliases){
					injectAlias(alloc, alias, as);
				}
				System.out.println(aliases);
			}
		}
		
		if(activeAllocationSiteAtFieldWrite.get(as).contains(alloc)){
			injectAlias(alloc, target, as);
		} else{
			allAllocationSiteAtFieldWrite.put(alloc,target);
		}
	}
	private void injectAlias(Node<Statement, Value> alloc, Node<Statement, Value> alias, AssignStmt as) {
		System.out.println("INJECTION ALIAS " + alias.fact() + " aliases " + alloc.fact() +" " + as);
	}
	private BiDiInterproceduralCFG<Unit, SootMethod> bwicfg() {
		if(bwicfg == null)
			bwicfg = new BackwardsInterproceduralCFG(icfg());
		return bwicfg;
	}
	protected void addForwardQuery(ForwardQuery query) {
		forwardSolve(query);
	}
	protected void addBackwardQuery(BackwardQuery backwardQuery, SootField field, Node<Statement, Value> node) {
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
				forwardSolver.solve(new Node<Statement,Value>(new Statement((Stmt) succ, icfg().getMethodOf(succ)), query.asNode().fact()));
			}
		}
	}

	public abstract BiDiInterproceduralCFG<Unit, SootMethod> icfg();

	public Collection<? extends Node<Statement, Value>> getForwardReachableStates() {
		return forwardSolver.getReachedStates();
	}
	
		
	private static class Alloc extends Node<Statement,Value>{
		public Alloc(Statement stmt, Value variable) {
			super(stmt, variable);
		}
	}
}
