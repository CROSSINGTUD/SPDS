package boomerang.poi;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.MethodBasedFieldTransitionListener;
import soot.SootMethod;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public class ExecuteImportFieldStmtPOI<W extends Weight> extends AbstractExecuteImportPOI<W> {

	private final Val baseVar;
	private final Val storedVar;
	private boolean activate;
	
	public ExecuteImportFieldStmtPOI(final AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver,
			AbstractPOI<Statement, Val, Field> poi, Statement succ) {
		super(baseSolver, flowSolver, poi.getStmt(), succ);
		this.baseVar = poi.getBaseVar();
		this.storedVar = poi.getStoredVar();
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
					if (alias.equals(baseVar) && t.getLabel().equals(Field.empty())) {
						// t.getTarget is the allocation site
						flowSolver.getFieldAutomaton().registerListener(new FindNode(curr.getMethod()));
					}
				}
			}
		});
	}
	
	private class FindNode extends MethodBasedFieldTransitionListener<W>{

		public FindNode(SootMethod method) {
			super(method);
		}

		@Override
		public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {

			if(activate)
				return;
			final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
			if (!t.getStart().fact().stmt().equals(succ))
				return;
			if (!(aliasedVariableAtStmt instanceof GeneratedState)) {
				Val alias = aliasedVariableAtStmt.fact().fact();
				if (alias.equals(baseVar)) {
					activate(t);
				}
			}
		}
	}

	@Override
	protected void activate(Transition<Field, INode<Node<Statement, Val>>> aliasTrans) {
//		if(activate)
//			return;
//		activate = true;
		baseSolver.getFieldAutomaton().registerListener(new WPAUpdateListener<Field,INode<Node<Statement,Val>>,W>(){
			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
					WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
				if (!(t.getStart() instanceof GeneratedState) && t.getStart().fact().stmt().equals(succ)
						&& !t.getStart().fact().fact().equals(baseVar)) {
					Val alias = t.getStart().fact().fact();
					Node<Statement, Val> aliasedVarAtSucc = new Node<Statement, Val>(succ, alias);
					Node<Statement, Val> rightOpNode = new Node<Statement, Val>(curr, storedVar);
					flowSolver.setFieldContextReachable(aliasedVarAtSucc);
					flowSolver.addNormalCallFlow(rightOpNode, aliasedVarAtSucc);
					importToFlowSolver(t,aliasTrans);
				}
			}});
	}

	protected void importToFlowSolver(Transition<Field, INode<Node<Statement, Val>>> t, Transition<Field, INode<Node<Statement, Val>>> aliasTrans) {
		if(t.getLabel().equals(Field.empty())) {
			flowSolver.getFieldAutomaton().addTransition(new Transition<Field,INode<Node<Statement,Val>>>(t.getStart(),aliasTrans.getLabel(),aliasTrans.getTarget()));
		} else if(!t.getLabel().equals(Field.epsilon())){
			flowSolver.getFieldAutomaton().addTransition(t);
			baseSolver.getFieldAutomaton().registerListener(new ImportToFlowSolver(t.getTarget(),aliasTrans));
		}
	}

	private class ImportToFlowSolver extends WPAStateListener<Field,INode<Node<Statement,Val>>,W>{

		private Transition<Field, INode<Node<Statement, Val>>> aliasTrans;

		public ImportToFlowSolver(INode<Node<Statement, Val>> node, Transition<Field, INode<Node<Statement, Val>>> aliasTrans) {
			super(node);
			this.aliasTrans = aliasTrans;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			importToFlowSolver(t,aliasTrans);
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

		private ExecuteImportFieldStmtPOI getOuterType() {
			return ExecuteImportFieldStmtPOI.this;
		}
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((baseVar == null) ? 0 : baseVar.hashCode());
		result = prime * result + ((storedVar == null) ? 0 : storedVar.hashCode());
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
		if (baseVar == null) {
			if (other.baseVar != null)
				return false;
		} else if (!baseVar.equals(other.baseVar))
			return false;
		if (storedVar == null) {
			if (other.storedVar != null)
				return false;
		} else if (!storedVar.equals(other.storedVar))
			return false;
		return true;
	}

}