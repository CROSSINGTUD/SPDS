package boomerang.poi;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.MethodBasedFieldTransitionListener;
import boomerang.solver.StatementBasedCallTransitionListener;
import boomerang.solver.StatementBasedFieldTransitionListener;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
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
	private final class ImportTransitionFromCall extends StatementBasedCallTransitionListener<W> {
		private AbstractBoomerangSolver<W> flowSolver;
		private Statement stmt;
		private INode<Val> target;

		public ImportTransitionFromCall(AbstractBoomerangSolver<W> flowSolver, Statement stmt, INode<Val> target) {
			super(stmt);
			this.flowSolver = flowSolver;
			this.stmt = stmt;
			this.target = target;
		}

		@Override
		public void onAddedTransition(Transition<Statement, INode<Val>> t) {
			if(t.getStart() instanceof GeneratedState)
				return;
			if(t.getLabel().equals(stmt)){
				flowSolver.getCallAutomaton().addTransition(new Transition<Statement, INode<Val>>(t.getStart(), t.getLabel(), target));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
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
			ImportTransitionFromCall other = (ImportTransitionFromCall) obj;
			if (flowSolver == null) {
				if (other.flowSolver != null)
					return false;
			} else if (!flowSolver.equals(other.flowSolver))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;
		}

		
	}
	private class ForAnyCallSiteOrExitStmt implements WPAUpdateListener<Statement, INode<Val>, W> {

		private ExecuteImportFieldStmtPOI<W> poi;

		public ForAnyCallSiteOrExitStmt(ExecuteImportFieldStmtPOI<W> poi) {
			this.poi = poi;
		}

		@Override
		public void onWeightAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> aut) {
			if(t.getStart() instanceof GeneratedState)
				return;
			Statement returnSiteOrExitStmt = t.getString();
			if(!returnSiteOrExitStmt.getUnit().isPresent()){
				return;
			}
			boolean predIsCallStmt = false;
			for (Statement s : flowSolver.getPredsOf(returnSiteOrExitStmt)) {
				predIsCallStmt |= s.isCallsite() && flowSolver.valueUsedInStatement(s.getUnit().get(), t.getStart().fact());
			}
			
			if (predIsCallStmt || (isBackward() && icfg.isExitStmt(returnSiteOrExitStmt.getUnit().get()))) {
				importSolvers(returnSiteOrExitStmt,t.getTarget());
			}
		}

		private void importSolvers(Statement callSiteOrExitStmt, INode<Val> node) {
			baseSolver.registerStatementCallTransitionListener(new ImportTransitionFromCall(flowSolver,callSiteOrExitStmt, node));
			baseSolver.registerStatementFieldTransitionListener(new CallSiteOrExitStmtImport(flowSolver, callSiteOrExitStmt, ExecuteImportFieldStmtPOI.this));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((poi == null) ? 0 : poi.hashCode());
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
			ForAnyCallSiteOrExitStmt other = (ForAnyCallSiteOrExitStmt) obj;
			if (poi == null) {
				if (other.poi != null)
					return false;
			} else if (!poi.equals(other.poi))
				return false;
			return true;
		}

	}

	protected final AbstractBoomerangSolver<W> baseSolver;
	protected final AbstractBoomerangSolver<W> flowSolver;
	protected final Statement curr;
	protected final Statement succ;
	protected final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> baseAutomaton;
	protected final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> flowAutomaton;
	private final Val baseVar;
	private final Val storedVar;
	private final Field field;
	private final BiDiInterproceduralCFG<Unit, SootMethod> icfg;
	boolean active = false;

	public ExecuteImportFieldStmtPOI(BiDiInterproceduralCFG<Unit, SootMethod> icfg,
			final AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver,
			AbstractPOI<Statement, Val, Field> poi, Statement succ) {
		this.icfg = icfg;
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
		if(baseSolver.equals(flowSolver)){
			return;
		}
		baseSolver.registerFieldTransitionListener(new MethodBasedFieldTransitionListener<W>(curr.getMethod()) {

			@Override
			public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
				final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
				if (active)
					return;
				if (!t.getStart().fact().stmt().equals(curr))
					return;
				if (!(aliasedVariableAtStmt instanceof GeneratedState)) {
					Val alias = aliasedVariableAtStmt.fact().fact();
					if (alias.equals(baseVar) && t.getLabel().equals(Field.empty())) {
						active = true;
						flowsTo();
					}
				}
			}
		});
	}

	protected void flowsTo() {
		handlingAtFieldStatements();
		handlingAtCallSites();
	}

	private void handlingAtFieldStatements() {
		baseSolver.registerStatementFieldTransitionListener(new ImportIndirectAliases(succ, this) );
		flowSolver.registerStatementCallTransitionListener(new ImportIndirectCallAliases(curr, this) );
	}

	private final class ImportIndirectCallAliases extends StatementBasedCallTransitionListener<W>{

		private ExecuteImportFieldStmtPOI<W> poi;

		public ImportIndirectCallAliases(Statement stmt, ExecuteImportFieldStmtPOI<W> poi) {
			super(stmt);
			this.poi = poi;
		}

		@Override
		public void onAddedTransition(Transition<Statement, INode<Val>> t) {
			if(t.getStart().fact().equals(storedVar)){
				baseSolver.registerStatementCallTransitionListener(new ImportIndirectCallAliasesAtSucc(succ,this,t.getTarget()));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((poi == null) ? 0 : poi.hashCode());
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
			ImportIndirectCallAliases other = (ImportIndirectCallAliases) obj;
			if (poi == null) {
				if (other.poi != null)
					return false;
			} else if (!poi.equals(other.poi))
				return false;
			return true;
		}
	}

	private final class ImportIndirectCallAliasesAtSucc extends StatementBasedCallTransitionListener<W>{

		private INode<Val> target;
		private ExecuteImportFieldStmtPOI<W>.ImportIndirectCallAliases poi;

		public ImportIndirectCallAliasesAtSucc(
				Statement succ, ExecuteImportFieldStmtPOI<W>.ImportIndirectCallAliases poi, INode<Val> target) {
			super(succ);
			this.poi = poi;
			this.target = target;
		}

		@Override
		public void onAddedTransition(Transition<Statement, INode<Val>> t) {
			flowSolver.getCallAutomaton().addTransition(new Transition<Statement, INode<Val>>(t.getStart(), t.getLabel(), target));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((poi == null) ? 0 : poi.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
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
			ImportIndirectCallAliasesAtSucc other = (ImportIndirectCallAliasesAtSucc) obj;
			if (poi == null) {
				if (other.poi != null)
					return false;
			} else if (!poi.equals(other.poi))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;
		}

	}
	
	private void handlingAtCallSites() {
		flowSolver.getCallAutomaton().registerListener(new ForAnyCallSiteOrExitStmt(this));
	}

	protected boolean isBackward() {
		return flowSolver instanceof BackwardBoomerangSolver;
	}

	private final class ImportIndirectAliases extends StatementBasedFieldTransitionListener<W> {
		private ExecuteImportFieldStmtPOI<W> poi;

		public ImportIndirectAliases(Statement succ, ExecuteImportFieldStmtPOI<W> poi) {
			super(succ);
			this.poi = poi;
		}

		@Override
		public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
			if(t.getLabel().equals(Field.epsilon())){
				return;
			}
			if (!(t.getStart() instanceof GeneratedState) && t.getStart().fact().stmt().equals(succ)) {
				importStartingFrom(t);
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((poi == null) ? 0 : poi.hashCode());
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
			ImportIndirectAliases other = (ImportIndirectAliases) obj;
			if (poi == null) {
				if (other.poi != null)
					return false;
			} else if (!poi.equals(other.poi))
				return false;
			return true;
		}

	}
	private final class CallSiteOrExitStmtImport extends StatementBasedFieldTransitionListener<W> {
		private ExecuteImportFieldStmtPOI<W> poi;

		private CallSiteOrExitStmtImport(AbstractBoomerangSolver<W> flowSolver,
				 Statement stmt, ExecuteImportFieldStmtPOI<W> poi) {
			super(stmt);
			this.poi = poi;
		}
		@Override
		public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> innerT) {
			if(innerT.getLabel().equals(Field.epsilon())){
				return;
			}
			if (!(innerT.getStart() instanceof GeneratedState)) {
				importStartingFrom(innerT);
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((poi == null) ? 0 : poi.hashCode());
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
			CallSiteOrExitStmtImport other = (CallSiteOrExitStmtImport) obj;
			if (poi == null) {
				if (other.poi != null)
					return false;
			} else if (!poi.equals(other.poi))
				return false;
			return true;
		}

	}

	protected void importStartingFrom(Transition<Field, INode<Node<Statement, Val>>> t) {
		if (t.getLabel().equals(Field.empty())) {
			INode<Node<Statement, Val>> intermediateState = flowSolver.getFieldAutomaton()
					.createState(new SingleNode<Node<Statement, Val>>(new Node<Statement, Val>(succ, baseVar)), field);
			flowSolver.getFieldAutomaton().addTransition(
					new Transition<Field, INode<Node<Statement, Val>>>(t.getStart(), field, intermediateState));
		} else  {
			flowSolver.getFieldAutomaton().addTransition(t);
			baseSolver.getFieldAutomaton()
					.registerListener(new ImportToSolver(t.getTarget(), this.baseSolver, this.flowSolver, this));
		}
	}

	private final class ImportToSolver extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

		private ExecuteImportFieldStmtPOI<W> poi;

		public ImportToSolver(INode<Node<Statement, Val>> target, AbstractBoomerangSolver<W> baseSolver,
				AbstractBoomerangSolver<W> flowSolver, ExecuteImportFieldStmtPOI<W> poi) {
			super(target);
			this.poi = poi;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			importStartingFrom(t);
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((baseSolver == null) ? 0 : baseSolver.hashCode());
			result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
			result = prime * result + ((poi == null) ? 0 : poi.hashCode());
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
			if (poi == null) {
				if (other.poi != null)
					return false;
			} else if (!poi.equals(other.poi))
				return false;
			return true;
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseSolver == null) ? 0 : baseSolver.hashCode());
		result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
		result = prime * result + ((curr == null) ? 0 : curr.hashCode());
//		result = prime * result + ((succ == null) ? 0 : succ.hashCode());
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
//		if (succ == null) {
//			if (other.succ != null)
//				return false;
//		} else if (!succ.equals(other.succ))
//			return false;
		return true;
	}

}