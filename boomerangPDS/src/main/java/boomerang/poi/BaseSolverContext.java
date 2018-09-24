package boomerang.poi;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;

public abstract class BaseSolverContext<W extends Weight> extends WPAStateListener<Statement, INode<Val>, W> {
	private final Object hashObject;
	private AbstractBoomerangSolver<W> solver;
	private Statement curr;

	public BaseSolverContext(Object hashObject, INode<Val> state, Statement curr, AbstractBoomerangSolver<W> solver) {
		super(state);
		this.hashObject = hashObject;
		this.curr = curr;
		this.solver = solver;
	}

	@Override
	public void onOutTransitionAdded(Transition<Statement, INode<Val>> t, W w,
			WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
		if (t.getTarget() instanceof GeneratedState) {
			if (!isBackward() && t.getLabel().equals(curr) || isBackward()
					&& solver.getPredsOf(curr).contains(t.getLabel())) {
				weightedPAutomaton.registerListener(new GetCallSite(t.getTarget(), solver) {
					@Override
					void callSiteFound(Statement label) {
						BaseSolverContext.this.callSiteFound(label);
					}
				});
			}
		} else if (t.getTarget().equals(weightedPAutomaton.getInitialState())) {
			anyContext();
		}
	}

	private boolean isBackward() {
		return solver instanceof BackwardBoomerangSolver;
	}

	public abstract void anyContext();

	public abstract void callSiteFound(Statement callSite);

	abstract class GetCallSite extends WPAStateListener<Statement, INode<Val>, W> {

		public GetCallSite(INode<Val> state, AbstractBoomerangSolver<W> solver) {
			super(state);
		}

		@Override
		public void onOutTransitionAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
			callSiteFound(t.getLabel());
		}

		abstract void callSiteFound(Statement label);

		@Override
		public void onInTransitionAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {

		}

	}

	@Override
	public void onInTransitionAdded(Transition<Statement, INode<Val>> t, W w,
			WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((hashObject == null) ? 0 : hashObject.hashCode());
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
		BaseSolverContext<W> other = (BaseSolverContext<W>) obj;
		if (hashObject == null) {
			if (other.hashObject != null)
				return false;
		} else if (!hashObject.equals(other.hashObject))
			return false;
		return true;
	}

}