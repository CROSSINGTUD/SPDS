package boomerang.poi;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.StatementBasedFieldTransitionListener;
import sync.pds.solver.SyncStatePDSUpdateListener;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.Transition;
import wpds.impl.Weight;

public class ExecuteImportCallStmtPOI<W extends Weight> {

	private AbstractBoomerangSolver<W> baseSolver;
	private AbstractBoomerangSolver<W> flowSolver;
	private Statement callSite;
	private Node<Statement, Val> returnedNode;

	public ExecuteImportCallStmtPOI(AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver,
			Statement callSite, Node<Statement, Val> returnedNode) {
		this.baseSolver = baseSolver;
		this.flowSolver = flowSolver;
		this.callSite = callSite;
		this.returnedNode = returnedNode;
	}

	public void solve() {
		//Returned node is a node that is returned from the flowSolver. We check, if the baseSolver contains the same node. 
		baseSolver.registerListener(new OnReturnNodeReachle(
				new WitnessNode<Statement, Val, Field>(returnedNode.stmt(), returnedNode.fact())));
	}

	private class OnReturnNodeReachle extends SyncStatePDSUpdateListener<Statement, Val, Field> {

		public OnReturnNodeReachle(WitnessNode<Statement, Val, Field> node) {
			super(node);
		}

		@Override
		public void reachable() {
			baseSolver.registerStatementFieldTransitionListener(new StatementBasedFieldTransitionListener<W>(returnedNode.stmt()) {

				@Override
				public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
					flowSolver.getFieldAutomaton().registerListener(new ImportToAutomatonWithNewStart<W>(flowSolver.getFieldAutomaton(),new SingleNode<Node<Statement,Val>>(returnedNode),t.getStart()));
					flowSolver.setFieldContextReachable(t.getStart().fact());
					flowSolver.addNormalCallFlow(returnedNode,t.getStart().fact());					
				}
			});
		}

	}

}
