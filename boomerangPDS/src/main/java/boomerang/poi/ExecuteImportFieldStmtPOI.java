package boomerang.poi;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.poi.ExecuteImportFieldStmtPOI.CopyTargets;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.StatementBasedFieldTransitionListener;
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
	public class CopyTargets extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

		private INode<Node<Statement, Val>> newStart;

		public CopyTargets(INode<Node<Statement, Val>> start, INode<Node<Statement, Val>> newStart) {
			super(start);
			this.newStart = newStart;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			flowSolver.getFieldAutomaton().addTransition(new Transition<Field, INode<Node<Statement, Val>>>(newStart, t.getLabel(),t.getTarget()));
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
			CopyTargets other = (CopyTargets) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (newStart == null) {
				if (other.newStart != null)
					return false;
			} else if (!newStart.equals(other.newStart))
				return false;
			return true;
		}

		private ExecuteImportFieldStmtPOI getOuterType() {
			return ExecuteImportFieldStmtPOI.this;
		}

	}

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
		baseSolver.registerStatementFieldTransitionListener(new StatementBasedFieldTransitionListener<W>(succOfWrite) {

			@Override
			public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
				final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
				if (!(aliasedVariableAtStmt instanceof GeneratedState)) {
					Val alias = aliasedVariableAtStmt.fact().fact();
					if (alias.equals(getBaseVar()) && t.getLabel().equals(Field.empty())) {
						// t.getTarget is the allocation site
						WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> baseAutomaton = baseSolver
								.getFieldAutomaton();
						baseAutomaton.registerListener(new ImportBackwards(t.getTarget()) {

							@Override
							public void trigger(Transition<Field, INode<Node<Statement, Val>>> triggerTrans) {
								 flowSolver.getFieldAutomaton().registerListener(new CopyTargets(t.getStart(),triggerTrans.getStart()));
							}
						});
					}

				}
			}
		});
		}
	}

	abstract class ImportBackwards extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

		public ImportBackwards(INode<Node<Statement, Val>> iNode) {
			super(iNode);
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if (!(t.getStart() instanceof GeneratedState) && t.getStart().fact().stmt().equals(getStmt())
					&& !t.getStart().fact().fact().equals(getBaseVar())) {
				Val alias = t.getStart().fact().fact();
				for (final Statement succOfWrite : flowSolver.getSuccsOf(getStmt())) {
					Node<Statement, Val> aliasedVarAtSucc = new Node<Statement, Val>(succOfWrite, alias);
					Node<Statement, Val> rightOpNode = new Node<Statement, Val>(getStmt(), getStoredVar());
					trigger(new Transition<Field, INode<Node<Statement, Val>>>(
							new SingleNode<Node<Statement, Val>>(aliasedVarAtSucc), t.getLabel(), t.getTarget()));
					flowSolver.setFieldContextReachable(aliasedVarAtSucc);
					flowSolver.addNormalCallFlow(rightOpNode, aliasedVarAtSucc);
				}
			}
			if (t.getStart() instanceof GeneratedState) {
				baseSolver.getFieldAutomaton().registerListener(new ImportBackwards(t.getStart()) {

					@Override
					public void trigger(Transition<Field, INode<Node<Statement, Val>>> innerT) {
						flowSolver.getFieldAutomaton().addTransition(innerT);
						ImportBackwards.this.trigger(t);
					}
				});
			}
		}

		public abstract void trigger(Transition<Field, INode<Node<Statement, Val>>> t);

		@Override
		public int hashCode() {
			return System.identityHashCode(this);
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}
	}
	
	
}