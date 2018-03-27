package boomerang.poi;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;

public abstract class AbstractExecuteImportPOI<W extends Weight> {

	protected final AbstractBoomerangSolver<W> baseSolver;
	protected final AbstractBoomerangSolver<W> flowSolver;
	protected final Statement curr;
	protected final Statement succ;

	public AbstractExecuteImportPOI(AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver,
			Statement curr, Statement succ) {
		this.baseSolver = baseSolver;
		this.flowSolver = flowSolver;
		this.curr = curr;
		this.succ = succ;
	}

	
	protected class TransitiveCallback implements Callback {

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
	}

	protected class DirectCallback implements Callback{

		private INode<Node<Statement, Val>> start;

		public DirectCallback(INode<Node<Statement, Val>> start) {
			this.start = start;
		}

		@Override
		public void trigger(Transition<Field, INode<Node<Statement, Val>>> t) {
			flowSolver.getFieldAutomaton().registerListener(
					new ImportToAutomatonWithNewStart(start, t.getStart()));
		}
	}

	protected interface Callback {
		public void trigger(Transition<Field, INode<Node<Statement, Val>>> t);
	}
	
	
	protected class ImportToAutomatonWithNewStart extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {
		
		private final INode<Node<Statement, Val>> newStart;

		public ImportToAutomatonWithNewStart(INode<Node<Statement, Val>> start, INode<Node<Statement, Val>> replacement) {
			super(start);
			this.newStart = replacement;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			flowSolver.getFieldAutomaton().addTransition(
					new Transition<Field, INode<Node<Statement, Val>>>(newStart, t.getLabel(), t.getTarget()));
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
			result = prime * result + ((newStart == null) ? 0 : newStart.hashCode());
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
			ImportToAutomatonWithNewStart other = (ImportToAutomatonWithNewStart) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (newStart == null) {
				if (other.newStart != null)
					return false;
			} else if (!newStart.equals(other.newStart))
				return false;
			return true;
		}

		private AbstractExecuteImportPOI getOuterType() {
			return AbstractExecuteImportPOI.this;
		}

	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseSolver == null) ? 0 : baseSolver.hashCode());
		result = prime * result + ((curr == null) ? 0 : curr.hashCode());
		result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
		result = prime * result + ((succ == null) ? 0 : succ.hashCode());
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
		AbstractExecuteImportPOI other = (AbstractExecuteImportPOI) obj;
		if (baseSolver == null) {
			if (other.baseSolver != null)
				return false;
		} else if (!baseSolver.equals(other.baseSolver))
			return false;
		if (curr == null) {
			if (other.curr != null)
				return false;
		} else if (!curr.equals(other.curr))
			return false;
		if (flowSolver == null) {
			if (other.flowSolver != null)
				return false;
		} else if (!flowSolver.equals(other.flowSolver))
			return false;
		if (succ == null) {
			if (other.succ != null)
				return false;
		} else if (!succ.equals(other.succ))
			return false;
		return true;
	}

}
