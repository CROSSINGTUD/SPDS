package boomerang.poi;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
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
	protected final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> baseAutomaton;
	protected final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> flowAutomaton;

	public AbstractExecuteImportPOI(AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver,
			Statement curr, Statement succ) {
		this.baseSolver = baseSolver;
		this.flowSolver = flowSolver;
		this.baseAutomaton = baseSolver.getFieldAutomaton();
		this.flowAutomaton = flowSolver.getFieldAutomaton();
		this.curr = curr;
		this.succ = succ;
	}

	protected boolean isBackward() {
		return flowSolver instanceof BackwardBoomerangSolver;
	}
	
	protected abstract void activate(Transition<Field, INode<Node<Statement, Val>>> aliasTrans);

	private final class ActivateListener extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {
		private ActivateListener(INode<Node<Statement, Val>> state) {
			super(state);
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(t.getLabel().equals(Field.empty()))
				return;
			if(t.getLabel().equals(Field.epsilon()))
				return;
			activate(t);
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
			ActivateListener other = (ActivateListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			return true;
		}

		private AbstractExecuteImportPOI getOuterType() {
			return AbstractExecuteImportPOI.this;
		}
		
		
	}

	protected class HasOutTransitionWithSameLabel extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {
		private Field baseLabel;
		private INode<Node<Statement, Val>> baseT;

		protected HasOutTransitionWithSameLabel(INode<Node<Statement, Val>> state,Field label,
				INode<Node<Statement, Val>> target) {
			super(state);
			this.baseLabel = label;
			this.baseT = target;
		}


		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> flowT, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if (flowT.getLabel().equals(baseLabel)) {
				baseAutomaton.registerListener(new IntersectionListenerNoLabel(baseT, flowT.getTarget()));
			}
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
			result = prime * result + ((baseLabel == null) ? 0 : baseLabel.hashCode());
			result = prime * result + ((baseT == null) ? 0 : baseT.hashCode());
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
			HasOutTransitionWithSameLabel other = (HasOutTransitionWithSameLabel) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (baseLabel == null) {
				if (other.baseLabel != null)
					return false;
			} else if (!baseLabel.equals(other.baseLabel))
				return false;
			if (baseT == null) {
				if (other.baseT != null)
					return false;
			} else if (!baseT.equals(other.baseT))
				return false;
			return true;
		}


		private AbstractExecuteImportPOI getOuterType() {
			return AbstractExecuteImportPOI.this;
		}

	}
	
	protected class IntersectionListenerNoLabel extends WPAStateListener<Field, INode<Node<Statement, Val>>, W>{
		private INode<Node<Statement, Val>> flowState;

		public IntersectionListenerNoLabel(INode<Node<Statement, Val>> baseState, INode<Node<Statement, Val>> flowState) {
			super(baseState);
			this.flowState = flowState;
		}


		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> baseT, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(baseT.getLabel().equals(Field.epsilon()))
				return;
			if(baseT.getLabel().equals(Field.empty())) {
				activateFrom(flowState);
				return;
			}
//			flowAutomaton.registerListener(new HasOutTransitionWithSameLabel(flowState, baseT.getLabel(),baseT.getTarget()));
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
			result = prime * result + ((flowState == null) ? 0 : flowState.hashCode());
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
			IntersectionListenerNoLabel other = (IntersectionListenerNoLabel) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (flowState == null) {
				if (other.flowState != null)
					return false;
			} else if (!flowState.equals(other.flowState))
				return false;
			return true;
		}

		private AbstractExecuteImportPOI getOuterType() {
			return AbstractExecuteImportPOI.this;
		}
	}
	
	protected interface IntersectionCallback{
		void trigger(INode<Node<Statement, Val>> baseT,
				Transition<Field, INode<Node<Statement, Val>>> flowT);	
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

	public void activateFrom(INode<Node<Statement, Val>> flowState) {
		flowAutomaton.registerListener(new ActivateListener(flowState));
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
