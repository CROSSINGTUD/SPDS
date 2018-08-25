package boomerang.poi;

import boomerang.Util;
import boomerang.WeightedBoomerang;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.MethodBasedFieldTransitionListener;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public class ExecuteImportFieldStmtPOI<W extends Weight> {

	protected final AbstractBoomerangSolver<W> baseSolver;
	protected final AbstractBoomerangSolver<W> flowSolver;
	protected final Statement curr;
	protected final Statement succ;
	protected final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> baseAutomaton;
	protected final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> flowAutomaton;
	private final Val baseVar;
	private final Val storedVar;
	private final Field field;
	boolean active = false;
	
	public ExecuteImportFieldStmtPOI(final AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver,
			AbstractPOI<Statement, Val, Field> poi, Statement succ) {
		this.baseSolver = baseSolver;
		this.flowSolver = flowSolver;
		this.baseAutomaton = baseSolver.getFieldAutomaton();
		this.flowAutomaton = flowSolver.getFieldAutomaton();
		this.curr = poi.getStmt();
		this.succ = succ;
		this.baseVar = poi.getBaseVar();
		this.storedVar = poi.getStoredVar();
		this.field = poi.getField();
	}
	

	public void solve() {
		baseSolver.registerFieldTransitionListener(
				new MethodBasedFieldTransitionListener<W>(curr.getMethod()) {

					@Override
					public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
						final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
						if(active)
							return;
						if (!t.getStart().fact().stmt().equals(curr))
							return;
						if (!(aliasedVariableAtStmt instanceof GeneratedState)) {
							Val alias = aliasedVariableAtStmt.fact().fact();
							if (alias.equals(baseVar)
									&& t.getLabel().equals(Field.empty())) {
								active = true;
								flowsTo();
							}
						}
					}
				});
	}

	protected void flowsTo() {
		baseSolver.getFieldAutomaton()
				.registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {

					@Override
					public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
							WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
						// TODO Auto-generated method stub
						if (!(t.getStart() instanceof GeneratedState)
								&& t.getStart().fact().stmt().equals(succ)) {
							importStartingFrom(t);
							Val alias = t.getStart().fact().fact();
							Node<Statement, Val> aliasedVarAtSucc = new Node<Statement, Val>(succ, alias);
							Node<Statement, Val> rightOpNode = new Node<Statement, Val>(curr,
									storedVar);
							flowSolver.addNormalCallFlow(rightOpNode, aliasedVarAtSucc);
						}
					}
				});
		handlingAtCallSites();
	}


	private void handlingAtCallSites() {
		flowSolver.getFieldAutomaton().registerListener(new ImportAtCallSite(this));
	}
	
	private final class ImportAtCallSite implements WPAUpdateListener<Field, INode<Node<Statement, Val>>, W> {

		private ExecuteImportFieldStmtPOI<W> executeImportFieldStmtPOI;

		public ImportAtCallSite(ExecuteImportFieldStmtPOI<W> executeImportFieldStmtPOI) {
			this.executeImportFieldStmtPOI = executeImportFieldStmtPOI;
		}

		@Override
		public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
			Statement stmt = t.getStart().fact().stmt();
			for(Statement s : flowSolver.getPredsOf(stmt)){
				if(Util.isCallStmt(s.getUnit().get()) && flowSolver.valueUsedInStatement(s.getUnit().get(),t.getStart().fact().fact())){
					baseSolver.getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, W>() {

						@Override
						public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> innerT, W w,
								WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
							Statement baseStmt = innerT.getStart().fact().stmt();
							if(baseStmt.equals(stmt)){
								importStartingFrom(innerT);
								if (!(innerT.getStart() instanceof GeneratedState)) {
									Val alias = innerT.getStart().fact().fact();
									Node<Statement, Val> aliasedVarAtSucc = new Node<Statement, Val>(stmt, alias);
									flowSolver.addNormalCallFlow(t.getStart().fact(), aliasedVarAtSucc);
								}
							}
						}
					});
				}
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((executeImportFieldStmtPOI == null) ? 0 : executeImportFieldStmtPOI.hashCode());
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
			ImportAtCallSite other = (ImportAtCallSite) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (executeImportFieldStmtPOI == null) {
				if (other.executeImportFieldStmtPOI != null)
					return false;
			} else if (!executeImportFieldStmtPOI.equals(other.executeImportFieldStmtPOI))
				return false;
			return true;
		}

		private ExecuteImportFieldStmtPOI getOuterType() {
			return ExecuteImportFieldStmtPOI.this;
		}
		
	}


	protected void importStartingFrom(Transition<Field, INode<Node<Statement, Val>>> t) {
		if (t.getLabel().equals(Field.empty())) {
			INode<Node<Statement, Val>> intermediateState = flowSolver.getFieldAutomaton()
					.createState(
							new SingleNode<Node<Statement, Val>>(
									new Node<Statement, Val>(succ, baseVar)),
							field);
			flowSolver.getFieldAutomaton().addTransition(
					new Transition<Field, INode<Node<Statement, Val>>>(t.getStart(),
							field, intermediateState));
		} else {
			flowSolver.getFieldAutomaton().addTransition(t);
			baseSolver.getFieldAutomaton().registerListener(
					new ImportToSolver(t.getTarget(),this));
		}
	}

	private final class ImportToSolver extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

		private ExecuteImportFieldStmtPOI<W> executeImportFieldStmtPOI;

		private ImportToSolver(INode<Node<Statement, Val>> state, ExecuteImportFieldStmtPOI<W> executeImportFieldStmtPOI) {
			super(state);
			this.executeImportFieldStmtPOI = executeImportFieldStmtPOI;
		}

		@Override
		public void onOutTransitionAdded(
				Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			executeImportFieldStmtPOI.importStartingFrom(t);
		}

		@Override
		public void onInTransitionAdded(
				Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((executeImportFieldStmtPOI == null) ? 0 : executeImportFieldStmtPOI.hashCode());
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
			ImportToSolver other = (ImportToSolver) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (executeImportFieldStmtPOI == null) {
				if (other.executeImportFieldStmtPOI != null)
					return false;
			} else if (!executeImportFieldStmtPOI.equals(other.executeImportFieldStmtPOI))
				return false;
			return true;
		}

		private ExecuteImportFieldStmtPOI getOuterType() {
			return ExecuteImportFieldStmtPOI.this;
		}
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseSolver == null) ? 0 : baseSolver.hashCode());
		result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
		result = prime * result + ((curr == null) ? 0 : curr.hashCode());
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
		if (curr == null) {
			if (other.curr != null)
				return false;
		} else if (!curr.equals(other.curr))
			return false;
		if (succ == null) {
			if (other.succ != null)
				return false;
		} else if (!succ.equals(other.succ))
			return false;
		return true;
	}

}