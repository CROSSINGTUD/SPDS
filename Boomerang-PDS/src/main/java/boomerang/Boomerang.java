package boomerang;

import java.util.Collection;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Optional;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.nodes.Node;

public abstract class Boomerang {
	private final ForwardBoomerangSolver forwardSolver = new ForwardBoomerangSolver(icfg());
	private final BackwardBoomerangSolver backwardSolver = new BackwardBoomerangSolver(new BackwardsInterproceduralCFG(icfg()));
	private final Set<Node<Statement,Value>> forwardReachableNodes = Sets.newHashSet();
	private final Set<Node<Statement,Value>> backwardReachableNodes = Sets.newHashSet();
	
	public Boomerang(){
		forwardSolver.registerListener(new SyncPDSUpdateListener<Statement, Value, Field>() {
			@Override
			public void onReachableNodeAdded(Node<Statement, Value> reachableNode) {
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
		backwardSolver.solve(query.asNode());
	}

	private void forwardSolve(ForwardQuery query) {
		Optional<Stmt> unit = query.asNode().stmt().getUnit();
		if(unit.isPresent()){
			for(Unit succ : icfg().getSuccsOf(unit.get())){
				System.out.println("SOLVE + " +new Node<Statement,Value>(new Statement((Stmt) succ, icfg().getMethodOf(succ)), query.asNode().fact()));
				forwardSolver.solve(new Node<Statement,Value>(new Statement((Stmt) succ, icfg().getMethodOf(succ)), query.asNode().fact()));
			}
		}
	}

	public abstract BiDiInterproceduralCFG<Unit, SootMethod> icfg();

	public Collection<? extends Node<Statement, Value>> getForwardReachableStates() {
		return forwardSolver.getReachedStates();
	}
	
		
}
