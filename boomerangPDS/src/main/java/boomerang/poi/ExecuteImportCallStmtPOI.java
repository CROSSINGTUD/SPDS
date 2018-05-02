package boomerang.poi;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.MethodBasedFieldTransitionListener;
import soot.SootMethod;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public class ExecuteImportCallStmtPOI<W extends Weight> extends AbstractExecuteImportPOI<W> {

	private final Val returningFact;
	private boolean activate;
	private Node<Statement, Val> returnedNode;

	public ExecuteImportCallStmtPOI(AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver,
			Statement callSite, Node<Statement, Val> returnedNode) {
		super(baseSolver, flowSolver, callSite, returnedNode.stmt());
		this.returnedNode = returnedNode;
		this.returningFact = returnedNode.fact();
	}

	public void solve() {
		baseSolver.registerFieldTransitionListener(new MethodBasedFieldTransitionListener<W>(curr.getMethod()) {

			@Override
			public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
				final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
				if(activate)
					return;
				if (!t.getStart().fact().stmt().equals(curr))
					return;
				if (!(aliasedVariableAtStmt instanceof GeneratedState)) {
					Val alias = aliasedVariableAtStmt.fact().fact();
					if (alias.equals(returningFact) && t.getLabel().equals(Field.empty())) {
						// t.getTarget is the allocation site
						flowSolver.registerFieldTransitionListener(new FindNode(curr.getMethod()));
					}
					if (alias.equals(returningFact) && !t.getLabel().equals(Field.empty())
							&& !t.getLabel().equals(Field.epsilon())) {
						flowAutomaton.registerListener(
								new HasOutTransitionWithSameLabel(new SingleNode<Node<Statement,Val>>(returnedNode), t.getLabel(), t.getTarget()));
					}
				}
			}
		});
	}

	private class FindNode extends MethodBasedFieldTransitionListener<W> {

		public FindNode(SootMethod method) {
			super(method);
		}

		@Override
		public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
			final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
			if(activate)
				return;
			if (!t.getStart().fact().stmt().equals(succ))
				return;
			if (!(aliasedVariableAtStmt instanceof GeneratedState)) {
				Val alias = aliasedVariableAtStmt.fact().fact();
				if (alias.equals(returningFact)) {
					activate(t);
				}
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((returningFact == null) ? 0 : returningFact.hashCode());
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
		ExecuteImportCallStmtPOI other = (ExecuteImportCallStmtPOI) obj;
		if (returningFact == null) {
			if (other.returningFact != null)
				return false;
		} else if (!returningFact.equals(other.returningFact))
			return false;
		return true;
	}

	@Override
	protected void activate(Transition<Field, INode<Node<Statement, Val>>> aliasTrans) {
		if(activate)
			return;
		activate = true;
		baseSolver.getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {
			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
					WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
				if (!(t.getStart() instanceof GeneratedState) && t.getStart().fact().stmt().equals(succ)
						&& !t.getStart().fact().fact().equals(returningFact)) {
					Val alias = t.getStart().fact().fact();
					Node<Statement, Val> aliasedVarAtSucc = new Node<Statement, Val>(succ, alias);
					Node<Statement, Val> rightOpNode = new Node<Statement, Val>(succ, returningFact);
					flowSolver.setFieldContextReachable(aliasedVarAtSucc);
					flowSolver.addNormalCallFlow(rightOpNode, aliasedVarAtSucc);
					importToFlowSolver(t, aliasTrans);
				}

			}
		});
	}

	protected void importToFlowSolver(Transition<Field, INode<Node<Statement, Val>>> t,
			Transition<Field, INode<Node<Statement, Val>>> aliasTrans) {
		if (t.getLabel().equals(Field.empty())) {
			flowSolver.getFieldAutomaton().addTransition(new Transition<Field, INode<Node<Statement, Val>>>(
					t.getStart(), aliasTrans.getLabel(), aliasTrans.getTarget()));
		} else if (!t.getLabel().equals(Field.epsilon())) {
			flowSolver.getFieldAutomaton().addTransition(t);
			baseSolver.getFieldAutomaton().registerListener(new ImportToFlowSolver(t.getTarget(), aliasTrans));
		}
	}

	private class ImportToFlowSolver extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

		private Transition<Field, INode<Node<Statement, Val>>> aliasTrans;

		public ImportToFlowSolver(INode<Node<Statement, Val>> node,
				Transition<Field, INode<Node<Statement, Val>>> aliasTrans) {
			super(node);
			this.aliasTrans = aliasTrans;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			importToFlowSolver(t, aliasTrans);
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((aliasTrans == null) ? 0 : aliasTrans.hashCode());
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
			ImportToFlowSolver other = (ImportToFlowSolver) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (aliasTrans == null) {
				if (other.aliasTrans != null)
					return false;
			} else if (!aliasTrans.equals(other.aliasTrans))
				return false;
			return true;
		}

		private ExecuteImportCallStmtPOI getOuterType() {
			return ExecuteImportCallStmtPOI.this;
		}
	}

}
