package boomerang.poi;

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

public class ExecuteImportCallStmtPOI<W extends Weight> extends AbstractExecuteImportPOI<W>{

	private final Val returningFact;

	public ExecuteImportCallStmtPOI(AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver,
			Statement callSite, Node<Statement, Val> returnedNode) {
		super(baseSolver, flowSolver, callSite, returnedNode.stmt());
		this.returningFact = returnedNode.fact();
	}

	public void solve() {
		baseSolver.getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, W>() {

			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
					WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
				final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
				if(!t.getStart().fact().stmt().equals(succ))
					return;
				
				if (!(aliasedVariableAtStmt instanceof GeneratedState)) {
					Val alias = aliasedVariableAtStmt.fact().fact();
					if (alias.equals(returningFact) && t.getLabel().equals(Field.empty())) {
						// t.getTarget is the allocation site
						WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> baseAutomaton = baseSolver
								.getFieldAutomaton();
						baseAutomaton.registerListener(new ImportBackwards(t.getTarget(), new DirectCallback(t.getStart())));
					}
					if (alias.equals(returningFact) && !t.getLabel().equals(Field.empty()) && !t.getLabel().equals(Field.epsilon())) {
						// t.getTarget is the allocation site
						WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> baseAutomaton = baseSolver
								.getFieldAutomaton();
						/**
						 * TODO this is the performance bottleneck. Can we implemented a different solution?
						 * when we comment out the line below, the test case
						 * @test.cases.sets.HashMapsLongTest terminated within 110sec.
						 * otherwise a time out occurs after 300sec.
						 * Carefully investigate why we need the line below:
						 * Some of the test cases of 
						 * @test.cases.fields.ThreeFieldsTest fail.
						 */
						baseAutomaton.registerListener(new TransitiveVisitor(t.getTarget()));
					}
				}
				
			}
		});
	}
	
	private class TransitiveVisitor extends WPAStateListener<Field, INode<Node<Statement, Val>>, W>{

		public TransitiveVisitor(INode<Node<Statement, Val>> state) {
			super(state);
			baseSolver
				.getFieldAutomaton().registerListener(new ImportBackwards(state, new FieldStackCallback(state)));
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(t.getLabel().equals(Field.empty()) || t.getLabel().equals(Field.epsilon()))
				return;
			baseSolver.getFieldAutomaton().registerListener(new TransitiveVisitor(t.getTarget()));
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
			TransitiveVisitor other = (TransitiveVisitor) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			return true;
		}

		private ExecuteImportCallStmtPOI getOuterType() {
			return ExecuteImportCallStmtPOI.this;
		}
		
	}

	private class FieldStackCallback implements Callback{

		private INode<Node<Statement, Val>> start;

		public FieldStackCallback(INode<Node<Statement, Val>> start) {
			this.start = start;
		}

		@Override
		public void trigger(Transition<Field, INode<Node<Statement, Val>>> t) {
			flowSolver.getFieldAutomaton().addTransition(new Transition<>(t.getStart(),t.getLabel(),start));
		}
		
	}
	// COPIED 

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
			if (!(t.getStart() instanceof GeneratedState) && t.getStart().fact().stmt().equals(curr)
					&& !t.getStart().fact().fact().equals(returningFact)) {
				Val alias = t.getStart().fact().fact();
				Node<Statement, Val> aliasedVarAtSucc = new Node<Statement, Val>(succ, alias);
				Node<Statement, Val> rightOpNode = new Node<Statement, Val>(succ, returningFact);
				callback.trigger(new Transition<Field, INode<Node<Statement, Val>>>(
						new SingleNode<Node<Statement, Val>>(aliasedVarAtSucc), t.getLabel(), t.getTarget()));
				flowSolver.setFieldContextReachable(aliasedVarAtSucc);
				flowSolver.addNormalCallFlow(rightOpNode, aliasedVarAtSucc);
			}
			if (t.getStart() instanceof GeneratedState) {
				baseSolver.getFieldAutomaton().registerListener(new ImportBackwards(t.getStart(),new TransitiveCallback(callback, t)));
			}
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
			ImportBackwards other = (ImportBackwards) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			return true;
		}

		private ExecuteImportCallStmtPOI getOuterType() {
			return ExecuteImportCallStmtPOI.this;
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
	
}
