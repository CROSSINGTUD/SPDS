package boomerang.poi;

import boomerang.WeightedBoomerang;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.StatementBasedCallTransitionListener;
import boomerang.solver.StatementBasedFieldTransitionListener;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public abstract class ExecuteImportFieldStmtPOI<W extends Weight> {
	private final class ImportTransitionFromCall extends StatementBasedCallTransitionListener<W> {
		private AbstractBoomerangSolver<W> flowSolver;
		private INode<Val> target;
		private W w;

		public ImportTransitionFromCall(AbstractBoomerangSolver<W> flowSolver, Statement stmt, INode<Val> target, W w) {
			super(stmt);
			this.flowSolver = flowSolver;
			this.target = target;
			this.w = w;
		}

		@Override
		public void onAddedTransition(Transition<Statement, INode<Val>> t, W w) {
			if (t.getStart() instanceof GeneratedState)
				return;
			flowSolver.getCallAutomaton()
			.addWeightForTransition(new Transition<Statement, INode<Val>>(t.getStart(), t.getLabel(), target), this.w);
			
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			result = prime * result + ((w == null) ? 0 : w.hashCode());
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
			if (w == null) {
				if (other.w != null)
					return false;
			} else if (!w.equals(other.w))
				return false;
			return true;
		}

	}
	private final class ImportOnReachStatement implements SyncPDSUpdateListener<Statement, Val> {
		private final Statement callSiteOrExitStmt;
		private AbstractBoomerangSolver<W> flowSolver;

		private ImportOnReachStatement(AbstractBoomerangSolver<W> flowSolver, Statement callSiteOrExitStmt) {
			this.flowSolver = flowSolver;
			this.callSiteOrExitStmt = callSiteOrExitStmt;
		}

		@Override
		public void onReachableNodeAdded(Node<Statement, Val> reachableNode) {
			if(reachableNode.stmt().equals(callSiteOrExitStmt)) {
				baseSolver.registerStatementFieldTransitionListener(
						new CallSiteOrExitStmtImport(flowSolver, baseSolver, reachableNode));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((callSiteOrExitStmt == null) ? 0 : callSiteOrExitStmt.hashCode());
			result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
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
			ImportOnReachStatement other = (ImportOnReachStatement) obj;
			if (callSiteOrExitStmt == null) {
				if (other.callSiteOrExitStmt != null)
					return false;
			} else if (!callSiteOrExitStmt.equals(other.callSiteOrExitStmt))
				return false;
			if (flowSolver == null) {
				if (other.flowSolver != null)
					return false;
			} else if (!flowSolver.equals(other.flowSolver))
				return false;
			return true;
		}

		
	}
	private class ForAnyCallSiteOrExitStmt implements WPAUpdateListener<Statement, INode<Val>, W> {

		

		private AbstractBoomerangSolver<W> baseSolver;

		public ForAnyCallSiteOrExitStmt(AbstractBoomerangSolver<W> baseSolver) {
			this.baseSolver = baseSolver;
		}

		@Override
		public void onWeightAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> aut) {
			if (t.getStart() instanceof GeneratedState)
				return;
			if(t.getLabel().equals(Statement.epsilon())) {
				return;
			}
			Statement returnSiteOrExitStmt = t.getString();
			if (!returnSiteOrExitStmt.getUnit().isPresent() && returnSiteOrExitStmt.getUnit() != null) {
				return;
			}
			if(!boomerang.icfg().isReachable(t.getString().getUnit().get())) {
				return;
			}
			boolean predIsCallStmt = false;
			for (Statement s : flowSolver.getPredsOf(returnSiteOrExitStmt)) {
				predIsCallStmt |= s.isCallsite()
						&& flowSolver.valueUsedInStatement(s.getUnit().get(), t.getStart().fact());
			}

			if (predIsCallStmt) {
				importSolvers(returnSiteOrExitStmt, t.getTarget(), w);
			} else	if (isBackward() && boomerang.icfg().isExitStmt(returnSiteOrExitStmt.getUnit().get())) {
				for(Statement next : flowSolver.getSuccsOf(returnSiteOrExitStmt)) {
					importSolvers(next, t.getTarget(), w);
				}
			}
		}

		
		private void importSolvers(Statement callSiteOrExitStmt, INode<Val> node, W w) {
			baseSolver.registerStatementCallTransitionListener(
					new ImportTransitionFromCall(flowSolver, callSiteOrExitStmt, node, w));
			baseSolver.registerListener(new ImportOnReachStatement(flowSolver, callSiteOrExitStmt));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((baseSolver == null) ? 0 : baseSolver.hashCode());
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
			if (baseSolver == null) {
				if (other.baseSolver != null)
					return false;
			} else if (!baseSolver.equals(other.baseSolver))
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
	boolean active = false;
	private WeightedBoomerang<W> boomerang;

	public ExecuteImportFieldStmtPOI(WeightedBoomerang<W> boomerang,
			final AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver,
			AbstractPOI<Statement, Val, Field> poi, Statement succ) {
		this.boomerang = boomerang;
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
		if (baseSolver.equals(flowSolver)) {
			return;
		}
		baseSolver.registerStatementFieldTransitionListener(new BaseVarPointsTo(curr, this));
	}

	private class BaseVarPointsTo extends StatementBasedFieldTransitionListener<W> {

		private ExecuteImportFieldStmtPOI<W> poi;

		public BaseVarPointsTo(Statement curr, ExecuteImportFieldStmtPOI<W> executeImportFieldStmtPOI) {
			super(curr);
			this.poi = executeImportFieldStmtPOI;
		}

		@Override
		public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
			final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
			if (active)
				return;
			if (!(aliasedVariableAtStmt instanceof GeneratedState)) {
				Val alias = aliasedVariableAtStmt.fact().fact();
				if (alias.equals(poi.baseVar) && t.getLabel().equals(Field.empty())) {
					flowsTo();
				}
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
			BaseVarPointsTo other = (BaseVarPointsTo) obj;
			if (poi == null) {
				if (other.poi != null)
					return false;
			} else if (!poi.equals(other.poi))
				return false;
			return true;
		}

	}

	protected void flowsTo() {
		if(active)
			return;
		active = true;
		handlingAtFieldStatements();
		handlingAtCallSites();
	}

	private void handlingAtFieldStatements() {
		baseSolver.registerStatementFieldTransitionListener(
				new ImportIndirectAliases(succ, this.flowSolver, this.baseSolver));
		flowSolver.registerStatementCallTransitionListener(new ImportIndirectCallAliases(curr, this.flowSolver));
	}

	private void handlingAtCallSites() {
		flowSolver.getCallAutomaton().registerListener(new ForAnyCallSiteOrExitStmt(this.baseSolver));
	}

	private final class ImportIndirectCallAliases extends StatementBasedCallTransitionListener<W> {

		private AbstractBoomerangSolver<W> flowSolver;

		public ImportIndirectCallAliases(Statement stmt, AbstractBoomerangSolver<W> flowSolver) {
			super(stmt);
			this.flowSolver = flowSolver;
		}

		@Override
		public void onAddedTransition(Transition<Statement, INode<Val>> t, W w) {
			if (t.getStart().fact().equals(storedVar)) {
				baseSolver.registerStatementCallTransitionListener(
						new ImportIndirectCallAliasesAtSucc(succ, t.getTarget(), w));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
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
			ImportIndirectCallAliases other = (ImportIndirectCallAliases) obj;
			if (flowSolver == null) {
				if (other.flowSolver != null)
					return false;
			} else if (!flowSolver.equals(other.flowSolver))
				return false;
			return true;
		}
	}

	private final class ImportIndirectCallAliasesAtSucc extends StatementBasedCallTransitionListener<W> {

		private INode<Val> target;
		private W w;

		public ImportIndirectCallAliasesAtSucc(Statement succ, INode<Val> target, W w) {
			super(succ);
			this.target = target;
			this.w = w;
		}

		@Override
		public void onAddedTransition(Transition<Statement, INode<Val>> t, W w) {
			flowSolver.getCallAutomaton()
					.addWeightForTransition(new Transition<Statement, INode<Val>>(t.getStart(), t.getLabel(), target),this.w);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			result = prime * result + ((w == null) ? 0 : w.hashCode());
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
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			if (w == null) {
				if (other.w != null)
					return false;
			} else if (!w.equals(other.w))
				return false;
			return true;
		}

	}

	protected boolean isBackward() {
		return flowSolver instanceof BackwardBoomerangSolver;
	}

	private final class ImportIndirectAliases extends StatementBasedFieldTransitionListener<W> {

		private AbstractBoomerangSolver<W> flowSolver;
		private AbstractBoomerangSolver<W> baseSolver;

		public ImportIndirectAliases(Statement succ, AbstractBoomerangSolver<W> flowSolver,
				AbstractBoomerangSolver<W> baseSolver) {
			super(succ);
			this.flowSolver = flowSolver;
			this.baseSolver = baseSolver;
		}

		@Override
		public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
			if (t.getLabel().equals(Field.epsilon())) {
				return;
			}
			if (!(t.getStart() instanceof GeneratedState)) {
				importStartingFrom(t);
			}
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
			ImportIndirectAliases other = (ImportIndirectAliases) obj;
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

	private final class CallSiteOrExitStmtImport extends StatementBasedFieldTransitionListener<W> {

		private AbstractBoomerangSolver<W> flowSolver;
		private AbstractBoomerangSolver<W> baseSolver;
		private Val fact;

		private CallSiteOrExitStmtImport(AbstractBoomerangSolver<W> flowSolver, AbstractBoomerangSolver<W> baseSolver,
				Node<Statement, Val> reachableNode) {
			super(reachableNode.stmt());
			this.flowSolver = flowSolver;
			this.baseSolver = baseSolver;
			this.fact = reachableNode.fact();
		}

		@Override
		public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> innerT) {
			if (innerT.getLabel().equals(Field.epsilon())) {
				return;
			}
			if (!(innerT.getStart() instanceof GeneratedState) && innerT.getStart().fact().fact().equals(fact)) {
				importStartingFrom(innerT);
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((baseSolver == null) ? 0 : baseSolver.hashCode());
			result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
			result = prime * result + ((fact == null) ? 0 : fact.hashCode());
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
			CallSiteOrExitStmtImport other = (CallSiteOrExitStmtImport) obj;
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
			if (fact == null) {
				if (other.fact != null)
					return false;
			} else if (!fact.equals(other.fact))
				return false;
			return true;
		}
	}

	protected void importStartingFrom(Transition<Field, INode<Node<Statement, Val>>> t) {
		if(t.getLabel().equals(Field.epsilon())) {
			return;
		}
		if (t.getLabel().equals(Field.empty())) {
			activate(t.getStart());
		} else {
			flowSolver.getFieldAutomaton().addTransition(new Transition<Field, INode<Node<Statement, Val>>>(convert(t.getStart()),t.getLabel(),convert(t.getTarget())));	
			baseSolver.getFieldAutomaton().registerListener(new ImportToSolver(t.getTarget(), this.flowSolver));
		}
	}

	private INode<Node<Statement, Val>> convert(INode<Node<Statement, Val>> target) {
		if(!isBackward())
			return target;
		if(target instanceof GeneratedState) {
			GeneratedState<Node<Statement, Val>,Field> generatedState = (GeneratedState<Node<Statement, Val>,Field>) target;
			return new GeneratedState<Node<Statement,Val>, Field>(flowSolver.getFieldAutomaton().getInitialState(), generatedState.location());
		}
		return target;
	}

	public abstract void activate(INode<Node<Statement, Val>> start);

	public void trigger(INode<Node<Statement, Val>> start) {
		INode<Node<Statement, Val>> intermediateState = flowSolver.getFieldAutomaton()
				.createState(new SingleNode<Node<Statement, Val>>(new Node<Statement, Val>(succ, baseVar)), field);
		flowSolver.getFieldAutomaton()
				.addTransition(new Transition<Field, INode<Node<Statement, Val>>>(start, field, intermediateState));
	}

	private final class ImportToSolver extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

		private AbstractBoomerangSolver<W> flowSolver;

		public ImportToSolver(INode<Node<Statement, Val>> target, AbstractBoomerangSolver<W> flowSolver) {
			super(target);
			this.flowSolver = flowSolver;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(t.getLabel().equals(Field.epsilon()))
				return;
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
			ImportToSolver other = (ImportToSolver) obj;
			if (flowSolver == null) {
				if (other.flowSolver != null)
					return false;
			} else if (!flowSolver.equals(other.flowSolver))
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