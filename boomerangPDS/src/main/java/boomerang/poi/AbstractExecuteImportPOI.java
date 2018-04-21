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

	
	protected class IntersectionListener extends WPAStateListener<Field, INode<Node<Statement, Val>>, W>{

		private INode<Node<Statement, Val>> flowState;
		private Field label;
		private IntersectionCallback callback;

		public IntersectionListener(INode<Node<Statement, Val>> baseState, INode<Node<Statement, Val>> flowState, Field label, IntersectionCallback callback) {
			super(baseState);
			this.flowState = flowState;
			this.label = label;
			this.callback = callback;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> baseT, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(baseT.getLabel().equals(Field.epsilon()))
				return;
			if(!baseT.getLabel().equals(label))
				return;
			flowAutomaton.registerListener(new HasOutTransitionWithSameLabel(flowState,baseT.getLabel(), baseT.getTarget(), callback));
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
			result = prime * result + ((label == null) ? 0 : label.hashCode());
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
			IntersectionListener other = (IntersectionListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (flowState == null) {
				if (other.flowState != null)
					return false;
			} else if (!flowState.equals(other.flowState))
				return false;
			if (label == null) {
				if (other.label != null)
					return false;
			} else if (!label.equals(other.label))
				return false;
			return true;
		}

		private AbstractExecuteImportPOI getOuterType() {
			return AbstractExecuteImportPOI.this;
		}
	}

	private final class HasOutTransitionWithSameLabel extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {
		private IntersectionCallback callback;
		private Field baseLabel;
		private INode<Node<Statement, Val>> baseT;

		private HasOutTransitionWithSameLabel(INode<Node<Statement, Val>> state,Field label,
				INode<Node<Statement, Val>> target, IntersectionCallback callback) {
			super(state);
			this.baseLabel = label;
			this.baseT = target;
			this.callback = callback;
		}


		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> flowT, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if (flowT.getLabel().equals(baseLabel)) {
				callback.trigger(baseT, flowT);
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
			if (callback == null) {
				if (other.callback != null)
					return false;
			} else if (!callback.equals(other.callback))
				return false;
			return true;
		}


		private AbstractExecuteImportPOI getOuterType() {
			return AbstractExecuteImportPOI.this;
		}

	}
	private final class CallBackOnInTransition extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {
		private final  INode<Node<Statement, Val>> baseT;
		private IntersectionCallback callback;

		private CallBackOnInTransition(INode<Node<Statement, Val>> state,
				INode<Node<Statement, Val>> baseT, IntersectionCallback callback) {
			super(state);
			this.baseT = baseT;
			this.callback = callback;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> flowT, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			callback.trigger(baseT,t);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
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
			CallBackOnInTransition other = (CallBackOnInTransition) obj;
			if (!getOuterType().equals(other.getOuterType()))
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
		private IntersectionCallback callback;

		public IntersectionListenerNoLabel(INode<Node<Statement, Val>> baseState, INode<Node<Statement, Val>> flowState) {
			super(baseState);
			this.flowState = flowState;
			this.callback = new IntersectionCallback() {

				boolean triggered = false;
				@Override
				public void trigger(INode<Node<Statement, Val>> baseT,
						Transition<Field, INode<Node<Statement, Val>>> flowT) {
					if(triggered)
						return;
					triggered = true;
					//3.
					baseAutomaton.registerListener(createImportBackwards(baseT,new DirectCallback(flowT.getTarget())));
//					baseAutomaton.registerListener(new IntersectionListenerNoLabel(baseT, flowT.getTarget()));
				}
			};
		}


		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> baseT, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(baseT.getLabel().equals(Field.epsilon()))
				return;
			if(baseT.getLabel().equals(Field.empty())) {
				flowAutomaton.registerListener(new CallBackOnInTransition(flowState, baseT.getTarget(), callback));
				return;
			}
			flowAutomaton.registerListener(new HasOutTransitionWithSameLabel(flowState, baseT.getLabel(),baseT.getTarget(), callback));
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
	

	protected abstract WPAStateListener<Field, INode<Node<Statement, Val>>, W> createImportBackwards(
			INode<Node<Statement, Val>> target, Callback directCallback);

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
