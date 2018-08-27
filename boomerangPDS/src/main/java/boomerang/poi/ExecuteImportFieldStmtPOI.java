package boomerang.poi;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.MethodBasedFieldTransitionListener;
import boomerang.solver.StatementBasedFieldTransitionListener;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WitnessNode;
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
		baseSolver.registerStatementFieldTransitionListener(new ImportIndirectAliases(succ, flowSolver,curr) );
//		getFieldAutomaton().registerListener(new ImportIndirectAliases(flowSolver,curr));
		handlingAtCallSites();
	}

	private void handlingAtCallSites() {
		flowSolver.registerListener(new ShareCallSiteListener(baseSolver, flowSolver));
	}

	protected boolean isBackward() {
		return flowSolver instanceof BackwardBoomerangSolver;
	}

	private final class ImportIndirectAliases extends StatementBasedFieldTransitionListener<W> {
		private AbstractBoomerangSolver<W> flowSolver;
		private Statement curr;

		public ImportIndirectAliases(Statement succ, AbstractBoomerangSolver<W> flowSolver, Statement curr) {
			super(succ);
			this.flowSolver = flowSolver;
			this.curr = curr;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((curr == null) ? 0 : curr.hashCode());
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
			ImportIndirectAliases other = (ImportIndirectAliases) obj;
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
			return true;
		}

		@Override
		public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t) {
			if(t.getLabel().equals(Field.epsilon())){
				return;
			}
			if (!(t.getStart() instanceof GeneratedState) && t.getStart().fact().stmt().equals(succ)) {
				importStartingFrom(t);
				Val alias = t.getStart().fact().fact();
				Node<Statement, Val> aliasedVarAtSucc = new Node<Statement, Val>(succ, alias);
				Node<Statement, Val> rightOpNode = new Node<Statement, Val>(curr, storedVar);
				flowSolver.addNormalCallFlow(rightOpNode, aliasedVarAtSucc);
			}
		}
	}
	private final class CallSiteOrExitStmtImport extends StatementBasedFieldTransitionListener<W> {
		private final WitnessNode<Statement, Val, Field> reachableNode;
		private final Statement stmt;
		private AbstractBoomerangSolver<W> flowSolver;

		private CallSiteOrExitStmtImport(AbstractBoomerangSolver<W> flowSolver,
				WitnessNode<Statement, Val, Field> reachableNode, Statement stmt) {
			super(stmt);
			this.flowSolver = flowSolver;
			this.reachableNode = reachableNode;
			this.stmt = stmt;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
			result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
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
			if (flowSolver == null) {
				if (other.flowSolver != null)
					return false;
			} else if (!flowSolver.equals(other.flowSolver))
				return false;
			if (stmt == null) {
				if (other.stmt != null)
					return false;
			} else if (!stmt.equals(other.stmt))
				return false;
			return true;
		}

		@Override
		public void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> innerT) {
			if(innerT.getLabel().equals(Field.epsilon())){
				return;
			}
			importStartingFrom(innerT);
			if (!(innerT.getStart() instanceof GeneratedState)) {
				Val alias = innerT.getStart().fact().fact();
				Node<Statement, Val> aliasedVarAtSucc = new Node<Statement, Val>(stmt, alias);
				flowSolver.addNormalCallFlow(reachableNode.asNode(), aliasedVarAtSucc);
			}
		}
	}
	private final class ShareCallSiteListener implements SyncPDSUpdateListener<Statement, Val, Field> {
		private AbstractBoomerangSolver<W> baseSolver;
		private AbstractBoomerangSolver<W> flowSolver;

		public ShareCallSiteListener(AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver) {
			this.baseSolver = baseSolver;
			this.flowSolver = flowSolver;
		}

		@Override
		public void onReachableNodeAdded(WitnessNode<Statement, Val, Field> reachableNode) {
			Statement stmt = reachableNode.stmt();
			boolean predIsCallStmt = false;
			for (Statement s : flowSolver.getPredsOf(stmt)) {
				predIsCallStmt |= s.isCallsite() && flowSolver.valueUsedInStatement(s.getUnit().get(), reachableNode.fact());
			}
			if (predIsCallStmt || (isBackward() && icfg.isExitStmt(stmt.getUnit().get()))) {
				baseSolver.getFieldAutomaton()
						.registerListener(new CallSiteOrExitStmtImport(flowSolver, reachableNode, stmt));
			}

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((baseSolver == null) ? 0 : baseSolver.hashCode());
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
			ShareCallSiteListener other = (ShareCallSiteListener) obj;
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

	protected void importStartingFrom(Transition<Field, INode<Node<Statement, Val>>> t) {
		if (t.getLabel().equals(Field.empty())) {
			INode<Node<Statement, Val>> intermediateState = flowSolver.getFieldAutomaton()
					.createState(new SingleNode<Node<Statement, Val>>(new Node<Statement, Val>(succ, baseVar)), field);
			flowSolver.getFieldAutomaton().addTransition(
					new Transition<Field, INode<Node<Statement, Val>>>(t.getStart(), field, intermediateState));
		} else if (!t.getLabel().equals(Field.epsilon())) {
			flowSolver.getFieldAutomaton().addTransition(t);
			baseSolver.getFieldAutomaton()
					.registerListener(new ImportToSolver(t.getTarget(), this.baseSolver, this.flowSolver));
		}
	}

	private final class ImportToSolver extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

		private AbstractBoomerangSolver<W> baseSolver;
		private AbstractBoomerangSolver<W> flowSolver;

		public ImportToSolver(INode<Node<Statement, Val>> target, AbstractBoomerangSolver<W> baseSolver,
				AbstractBoomerangSolver<W> flowSolver) {
			super(target);
			this.baseSolver = baseSolver;
			this.flowSolver = flowSolver;
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

	@Override
	public int hashCode() {
		if(true && 1 + 1 == 2)
		throw new RuntimeException();
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