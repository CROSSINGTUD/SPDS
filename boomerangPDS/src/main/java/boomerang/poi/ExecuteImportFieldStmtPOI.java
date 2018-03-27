package boomerang.poi;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public class ExecuteImportFieldStmtPOI<W extends Weight> extends AbstractPOI<Statement, Val, Field> {
	private AbstractBoomerangSolver<W> baseSolver;
	private AbstractBoomerangSolver<W> flowSolver;

	public ExecuteImportFieldStmtPOI(final AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver,
			AbstractPOI<Statement, Val, Field> poi) {
		super(poi.getStmt(), poi.getBaseVar(), poi.getField(), poi.getStoredVar());
		this.baseSolver = baseSolver;
		this.flowSolver = flowSolver;
	}

	public void execute(ForwardQuery baseAllocation, Query flowAllocation) {

	}

	public void solve() {
		assert !flowSolver.getSuccsOf(getStmt()).isEmpty();

		for (final Statement succOfWrite : flowSolver.getSuccsOf(getStmt())) {
			baseSolver.getFieldAutomaton()
					.registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {

						@Override
						public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
								WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
							final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
							if (!t.getStart().fact().stmt().equals(succOfWrite))
								return;
							if (!(aliasedVariableAtStmt instanceof GeneratedState)) {
								Val alias = aliasedVariableAtStmt.fact().fact();
								if (alias.equals(getBaseVar()) && t.getLabel().equals(Field.empty())) {
									// t.getTarget is the allocation site
									WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> baseAutomaton = baseSolver
											.getFieldAutomaton();
									baseAutomaton.registerListener(
											new ImportBackwards(t.getTarget(), new DirectCallback(t.getStart())));
								}
							}
						}
					});
		}
	}

	private class DirectCallback implements Callback {

		private INode<Node<Statement, Val>> start;

		public DirectCallback(INode<Node<Statement, Val>> start) {
			this.start = start;
		}

		@Override
		public void trigger(Transition<Field, INode<Node<Statement, Val>>> t) {
			flowSolver.getFieldAutomaton().registerListener(
					new ImportToAutomatonWithNewStart<W>(flowSolver.getFieldAutomaton(), start, t.getStart(), this));

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((start == null) ? 0 : start.hashCode());
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
			DirectCallback other = (DirectCallback) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (start == null) {
				if (other.start != null)
					return false;
			} else if (!start.equals(other.start))
				return false;
			return true;
		}

		private ExecuteImportFieldStmtPOI getOuterType() {
			return ExecuteImportFieldStmtPOI.this;
		}

	}

	private class ImportBackwards extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

		private Callback callback;

		public ImportBackwards(INode<Node<Statement, Val>> iNode, Callback callback) {
			super(iNode);
			this.callback = callback;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(t.getLabel().equals(Field.epsilon()))
				return;
			if (!(t.getStart() instanceof GeneratedState) && t.getStart().fact().stmt().equals(getStmt())
					&& !t.getStart().fact().fact().equals(getBaseVar())) {
				Val alias = t.getStart().fact().fact();
				for (final Statement succOfWrite : flowSolver.getSuccsOf(getStmt())) {
					Node<Statement, Val> aliasedVarAtSucc = new Node<Statement, Val>(succOfWrite, alias);
					Node<Statement, Val> rightOpNode = new Node<Statement, Val>(getStmt(), getStoredVar());
					callback.trigger(new Transition<Field, INode<Node<Statement, Val>>>(
							new SingleNode<Node<Statement, Val>>(aliasedVarAtSucc), t.getLabel(), t.getTarget()));
					flowSolver.setFieldContextReachable(aliasedVarAtSucc);
					flowSolver.addNormalCallFlow(rightOpNode, aliasedVarAtSucc);
				}
			}
			if (t.getStart() instanceof GeneratedState) {
				baseSolver.getFieldAutomaton()
						.registerListener(new ImportBackwards(t.getStart(), new TransitiveCallback(callback, t)));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((callback == null) ? 0 : callback.hashCode());
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
			ImportBackwards other = (ImportBackwards) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (callback == null) {
				if (other.callback != null)
					return false;
			} else if (!callback.equals(other.callback))
				return false;
			return true;
		}

		private ExecuteImportFieldStmtPOI getOuterType() {
			return ExecuteImportFieldStmtPOI.this;
		}

	}

	private class TransitiveCallback implements Callback {

		private Callback parent;
		private Transition<Field, INode<Node<Statement, Val>>> t;

		public TransitiveCallback(Callback callback, Transition<Field, INode<Node<Statement, Val>>> t) {
			this.parent = callback;
			this.t = t;
		}

		@Override
		public void trigger(Transition<Field, INode<Node<Statement, Val>>> innerT) {
			flowSolver.getFieldAutomaton().addTransition(innerT);
			parent.trigger(t);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((t == null) ? 0 : t.hashCode());
//			result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
			TransitiveCallback other = (TransitiveCallback) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
//			 if (parent == null) {
//			 if (other.parent != null)
//			 return false;
//			 } else if (!parent.equals(other.parent))
//			 return false;
			if (t == null) {
				if (other.t != null)
					return false;
			} else if (!t.equals(other.t))
				return false;
			return true;
		}

		private ExecuteImportFieldStmtPOI getOuterType() {
			return ExecuteImportFieldStmtPOI.this;
		}

	}

	private interface Callback {
		public void trigger(Transition<Field, INode<Node<Statement, Val>>> t);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((baseSolver == null) ? 0 : baseSolver.hashCode());
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
		ExecuteImportFieldStmtPOI other = (ExecuteImportFieldStmtPOI) obj;
		if (baseSolver == null) {
			if (other.baseSolver != null)
				return false;
		} else if (!baseSolver.equals(other.baseSolver))
			return false;
		if (flowSolver == null) {
			if (other.flowSolver != null)
				return false;
		} else if (!flowSolver.equals(other.flowSolver))
			return false;
		return true;
	}

	
	
}