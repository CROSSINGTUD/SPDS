package boomerang.poi;

import boomerang.scene.Field;
import boomerang.scene.ReturnSiteStatement;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import boomerang.solver.StatementBasedCallTransitionListener;
import boomerang.solver.StatementBasedFieldTransitionListener;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteImportFieldStmtPOI.class);
  private static final int MAX_IMPORT_DEPTH = -1;
  private Set<INode<Node<Statement, Val>>> reachable = Sets.newHashSet();
  private Multimap<INode<Node<Statement, Val>>, InsertFieldTransitionCallback> delayedTransitions =
      HashMultimap.create();
  protected final ForwardBoomerangSolver<W> baseSolver;
  protected final ForwardBoomerangSolver<W> flowSolver;
  protected final Statement curr;
  protected final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> baseAutomaton;
  protected final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> flowAutomaton;
  private final Val baseVar;
  private final Val storedVar;
  private final Field field;
  boolean active = false;

  public ExecuteImportFieldStmtPOI(
      final ForwardBoomerangSolver<W> baseSolver,
      ForwardBoomerangSolver<W> flowSolver,
      AbstractPOI<Statement, Val, Field> poi) {
    this.baseSolver = baseSolver;
    this.flowSolver = flowSolver;
    this.baseAutomaton = baseSolver.getFieldAutomaton();
    this.flowAutomaton = flowSolver.getFieldAutomaton();
    this.curr = poi.getStmt();
    this.baseVar = poi.getBaseVar();
    this.storedVar = poi.getStoredVar();
    this.field = poi.getField();
  }

  private boolean isLogEnabled() {
    return true;
  }

  private final class ImportTransitionFromCall extends StatementBasedCallTransitionListener<W> {

    private final INode<Val> start;
    private AbstractBoomerangSolver<W> flowSolver;
    private INode<Val> target;
    private W w;

    public ImportTransitionFromCall(
        AbstractBoomerangSolver<W> flowSolver,
        Statement stmt,
        INode<Val> start,
        INode<Val> target,
        W w) {
      super(stmt);
      this.start = start;
      this.flowSolver = flowSolver;
      this.target = target;
      this.w = w;
    }

    @Override
    public void onAddedTransition(Transition<Statement, INode<Val>> t, W w) {
      if (t.getStart() instanceof GeneratedState) return;
      Transition<Statement, INode<Val>> newTrans =
          new Transition<Statement, INode<Val>>(t.getStart(), t.getLabel(), target);
      if (isLogEnabled()) {
        LOGGER.trace("Copying {} to {}", newTrans, flowSolver);
      }
      if (!t.getStart().equals(start)) {
        if (t.getStart().fact().m().equals(t.getLabel().getMethod())) {
          // To compute the right Data-Flow Path, apparently, Weight.ONE is necessary and the
          // following line works.
          // flowSolver.getCallAutomaton().addTransition(newTrans);
          // For IDEAL the Weight this.w must be carried along:
          // FileMustBeClosedTest.flowViaFieldDirect
          flowSolver.getCallAutomaton().addWeightForTransition(newTrans, this.w);
        }
      }
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
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ImportTransitionFromCall other = (ImportTransitionFromCall) obj;
      if (flowSolver == null) {
        if (other.flowSolver != null) return false;
      } else if (!flowSolver.equals(other.flowSolver)) return false;
      if (target == null) {
        if (other.target != null) return false;
      } else if (!target.equals(other.target)) return false;
      if (w == null) {
        if (other.w != null) return false;
      } else if (!w.equals(other.w)) return false;
      return true;
    }
  }

  private final class ImportOnReachStatement extends StatementBasedCallTransitionListener<W> {
    private AbstractBoomerangSolver<W> flowSolver;

    private ImportOnReachStatement(
        AbstractBoomerangSolver<W> flowSolver, Statement callSiteOrExitStmt) {
      super(callSiteOrExitStmt);
      this.flowSolver = flowSolver;
    }

    @Override
    public void onAddedTransition(Transition<Statement, INode<Val>> t, W w) {
      if (t.getStart() instanceof GeneratedState) {
        return;
      }
      if (t.getLabel().equals(getStmt())) {
        baseSolver.registerStatementFieldTransitionListener(
            new CallSiteOrExitStmtFieldImport(
                flowSolver, baseSolver, new Node<>(t.getLabel(), t.getStart().fact())));
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
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      ImportOnReachStatement other = (ImportOnReachStatement) obj;
      if (flowSolver == null) {
        if (other.flowSolver != null) return false;
      } else if (!flowSolver.equals(other.flowSolver)) return false;
      return true;
    }
  }

  private class ForAnyCallSiteOrExitStmt implements WPAUpdateListener<Statement, INode<Val>, W> {
    private AbstractBoomerangSolver<W> baseSolver;

    public ForAnyCallSiteOrExitStmt(AbstractBoomerangSolver<W> baseSolver) {
      this.baseSolver = baseSolver;
    }

    @Override
    public void onWeightAdded(
        Transition<Statement, INode<Val>> t,
        W w,
        WeightedPAutomaton<Statement, INode<Val>, W> aut) {
      if (!flowSolver.getCallAutomaton().isUnbalancedState(t.getTarget())) return;
      if (t.getLabel().equals(Statement.epsilon())) {
        return;
      }
      Statement returnSiteOrExitStmt = t.getLabel();
      /*  if (t.getStart().fact() instanceof AllocVal) {
        if (((AllocVal) t.getStart().fact())
            .getDelegate()
            .equals(t.getTarget().fact().asUnbalanced(null))) {
          return;
        }
      } else if (t.getStart()
          .fact()
          .asUnbalanced(null)
          .equals(t.getTarget().fact().asUnbalanced(null))) return;*/
      if (returnSiteOrExitStmt instanceof ReturnSiteStatement) {
        Statement callSite = ((ReturnSiteStatement) returnSiteOrExitStmt).getCallSiteStatement();
        if (returnSiteOrExitStmt.unwrap().isAssign()
            && returnSiteOrExitStmt.unwrap().getLeftOp().equals(t.getStart().fact())) return;
        if (callSite.containsInvokeExpr() && callSite.uses(t.getStart().fact())) {
          importSolvers(returnSiteOrExitStmt, t.getStart(), t.getTarget(), w);
        }
      }
    }

    private void importSolvers(
        Statement callSiteOrExitStmt, INode<Val> start, INode<Val> node, W w) {
      if (isLogEnabled()) {
        LOGGER.trace(
            "Importing solvers at {} from {} to {}", callSiteOrExitStmt, baseSolver, flowSolver);
      }
      baseSolver.registerStatementCallTransitionListener(
          new ImportOnReachStatement(flowSolver, callSiteOrExitStmt));
      baseSolver.registerStatementCallTransitionListener(
          new ImportTransitionFromCall(flowSolver, callSiteOrExitStmt, start, node, w));
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
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ForAnyCallSiteOrExitStmt other = (ForAnyCallSiteOrExitStmt) obj;
      if (baseSolver == null) {
        if (other.baseSolver != null) return false;
      } else if (!baseSolver.equals(other.baseSolver)) return false;
      return true;
    }
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
      if (active) return;
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
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      BaseVarPointsTo other = (BaseVarPointsTo) obj;
      if (poi == null) {
        if (other.poi != null) return false;
      } else if (!poi.equals(other.poi)) return false;
      return true;
    }
  }

  protected void flowsTo() {
    if (active) return;
    active = true;
    if (isLogEnabled()) {
      LOGGER.trace("POI: Propagation of {} flows to {}", baseSolver, flowSolver);
    }
    handlingAtFieldStatements();
    handlingAtCallSites();
  }

  private void handlingAtFieldStatements() {
    baseSolver.registerStatementFieldTransitionListener(
        new ImportIndirectAliases(curr, this.flowSolver, this.baseSolver));
    flowSolver.registerStatementCallTransitionListener(
        new ImportIndirectCallAliases(curr, this.flowSolver));
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
            new ImportIndirectCallAliasesAtSucc(curr, t.getTarget(), w));
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
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      ImportIndirectCallAliases other = (ImportIndirectCallAliases) obj;
      if (flowSolver == null) {
        if (other.flowSolver != null) return false;
      } else if (!flowSolver.equals(other.flowSolver)) return false;
      return true;
    }
  }

  private final class ImportIndirectCallAliasesAtSucc
      extends StatementBasedCallTransitionListener<W> {

    private INode<Val> target;
    private W w;

    public ImportIndirectCallAliasesAtSucc(Statement succ, INode<Val> target, W w) {
      super(succ);
      this.target = target;
      this.w = w;
    }

    @Override
    public void onAddedTransition(Transition<Statement, INode<Val>> t, W w) {
      if (getStmt().isFieldStore()
          && !getStmt().getFieldStore().getX().equals(t.getStart().fact())) {
        flowSolver
            .getCallAutomaton()
            .addWeightForTransition(new Transition<>(t.getStart(), t.getLabel(), target), this.w);
      }
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
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      ImportIndirectCallAliasesAtSucc other = (ImportIndirectCallAliasesAtSucc) obj;
      if (target == null) {
        if (other.target != null) return false;
      } else if (!target.equals(other.target)) return false;
      if (w == null) {
        if (other.w != null) return false;
      } else if (!w.equals(other.w)) return false;
      return true;
    }
  }

  private final class ImportIndirectAliases extends StatementBasedFieldTransitionListener<W> {

    private AbstractBoomerangSolver<W> flowSolver;
    private AbstractBoomerangSolver<W> baseSolver;

    public ImportIndirectAliases(
        Statement succ,
        AbstractBoomerangSolver<W> flowSolver,
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
        importFieldTransitionsStartingAt(t, 0);
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
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      ImportIndirectAliases other = (ImportIndirectAliases) obj;
      if (baseSolver == null) {
        if (other.baseSolver != null) return false;
      } else if (!baseSolver.equals(other.baseSolver)) return false;
      if (flowSolver == null) {
        if (other.flowSolver != null) return false;
      } else return flowSolver.equals(other.flowSolver);
      return true;
    }
  }

  private final class CallSiteOrExitStmtFieldImport
      extends StatementBasedFieldTransitionListener<W> {

    private AbstractBoomerangSolver<W> flowSolver;
    private AbstractBoomerangSolver<W> baseSolver;
    private Val fact;

    private CallSiteOrExitStmtFieldImport(
        AbstractBoomerangSolver<W> flowSolver,
        AbstractBoomerangSolver<W> baseSolver,
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
      if (!(innerT.getStart() instanceof GeneratedState)
          && innerT.getStart().fact().fact().equals(fact)) {
        importFieldTransitionsStartingAt(innerT, 0);
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
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      CallSiteOrExitStmtFieldImport other = (CallSiteOrExitStmtFieldImport) obj;
      if (baseSolver == null) {
        if (other.baseSolver != null) return false;
      } else if (!baseSolver.equals(other.baseSolver)) return false;
      if (flowSolver == null) {
        if (other.flowSolver != null) return false;
      } else if (!flowSolver.equals(other.flowSolver)) return false;
      if (fact == null) {
        if (other.fact != null) return false;
      } else if (!fact.equals(other.fact)) return false;
      return true;
    }
  }

  protected void importFieldTransitionsStartingAt(
      Transition<Field, INode<Node<Statement, Val>>> t, int importDepth) {
    if (MAX_IMPORT_DEPTH > 0 && importDepth > MAX_IMPORT_DEPTH) return;
    if (t.getLabel().equals(Field.epsilon())) {
      return;
    }
    if (t.getLabel().equals(Field.empty())) {
      if (isLogEnabled()) {
        LOGGER.trace("Activating with {}", t.getStart());
      }
      if (baseSolver.getFieldAutomaton().isUnbalancedState(t.getTarget())) {
        activate(t.getStart());
      }
    } else if (t.getTarget() instanceof GeneratedState) {
      if (isLogEnabled()) {
        LOGGER.trace("Copying {} into Field Automaton {}", t, flowSolver);
      }
      queueOrAdd(t);
      int newDepth = importDepth + 1;
      baseSolver
          .getFieldAutomaton()
          .registerListener(
              new ImportFieldTransitionsFrom(t.getTarget(), this.flowSolver, newDepth));
    }
  }

  public void addReachable(INode<Node<Statement, Val>> node) {
    if (reachable.add(node)) {
      for (InsertFieldTransitionCallback callback :
          Lists.newArrayList(delayedTransitions.get(node))) {
        callback.trigger();
      }
    }
  }

  private void queueOrAdd(Transition<Field, INode<Node<Statement, Val>>> transToInsert) {
    if (reachable.contains(transToInsert.getTarget())) {
      flowSolver.getFieldAutomaton().addTransition(transToInsert);
      addReachable(transToInsert.getStart());
    } else {
      delayedTransitions.put(
          transToInsert.getTarget(), new InsertFieldTransitionCallback(transToInsert));
    }
  }

  public abstract void activate(INode<Node<Statement, Val>> start);

  public void trigger(INode<Node<Statement, Val>> start) {
    INode<Node<Statement, Val>> intermediateState =
        flowSolver
            .getFieldAutomaton()
            .createState(new SingleNode<>(new Node<>(curr, baseVar)), field);
    Transition<Field, INode<Node<Statement, Val>>> connectingTrans =
        new Transition<Field, INode<Node<Statement, Val>>>(start, field, intermediateState);
    if (isLogEnabled()) {
      LOGGER.trace("Connecting {} into Field Automaton {}", connectingTrans, flowSolver);
    }
    flowSolver.getFieldAutomaton().addTransition(connectingTrans);
    addReachable(connectingTrans.getStart());
  }

  private final class ImportFieldTransitionsFrom
      extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

    private AbstractBoomerangSolver<W> flowSolver;
    private int importDepth;

    public ImportFieldTransitionsFrom(
        INode<Node<Statement, Val>> target,
        AbstractBoomerangSolver<W> flowSolver,
        int importDepth) {
      super(target);
      this.flowSolver = flowSolver;
      this.importDepth = importDepth;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
      if (t.getLabel().equals(Field.epsilon())) return;
      importFieldTransitionsStartingAt(t, importDepth);
    }

    @Override
    public void onInTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {}

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      ImportFieldTransitionsFrom other = (ImportFieldTransitionsFrom) obj;
      if (flowSolver == null) {
        if (other.flowSolver != null) return false;
      } else if (!flowSolver.equals(other.flowSolver)) return false;
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
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ExecuteImportFieldStmtPOI other = (ExecuteImportFieldStmtPOI) obj;
    if (baseSolver == null) {
      if (other.baseSolver != null) return false;
    } else if (!baseSolver.equals(other.baseSolver)) return false;
    if (flowSolver == null) {
      if (other.flowSolver != null) return false;
    } else if (!flowSolver.equals(other.flowSolver)) return false;
    if (curr == null) {
      if (other.curr != null) return false;
    } else if (!curr.equals(other.curr)) return false;
    return true;
  }

  private class InsertFieldTransitionCallback {
    private final Transition<Field, INode<Node<Statement, Val>>> trans;

    public InsertFieldTransitionCallback(Transition<Field, INode<Node<Statement, Val>>> trans) {
      this.trans = trans;
    }

    public void trigger() {
      flowSolver.getFieldAutomaton().addTransition(trans);
      addReachable(trans.getStart());
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((trans == null) ? 0 : trans.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      InsertFieldTransitionCallback other = (InsertFieldTransitionCallback) obj;
      if (trans == null) {
        if (other.trans != null) return false;
      } else if (!trans.equals(other.trans)) return false;
      return true;
    }
  }
}
