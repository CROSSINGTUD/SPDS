package boomerang.poi;

import boomerang.BackwardQuery;
import boomerang.scene.Field;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Set;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;

public class CopyAccessPathChain<W extends Weight> {
  private static final int MAX_WALK_DEPTH = -1;
  private ForwardBoomerangSolver<W> forwardSolver;
  private BackwardBoomerangSolver<W> backwardSolver;
  private Statement fieldWriteStatement;
  // TODO: Source of non-determinsim: not part of hashCode/equals....but also shall not be.
  private INode<Node<Statement, Val>> killedTransitionTarget;

  public CopyAccessPathChain(
      ForwardBoomerangSolver<W> forwardSolver,
      BackwardBoomerangSolver<W> backwardSolver,
      Statement fieldWriteStatement,
      Transition<Field, INode<Node<Statement, Val>>> killedTransition) {
    this.forwardSolver = forwardSolver;
    this.backwardSolver = backwardSolver;
    this.fieldWriteStatement = fieldWriteStatement;
    this.killedTransitionTarget = killedTransition.getTarget();
  }

  public void exec() {
    forwardSolver
        .getFieldAutomaton()
        .registerListener(
            new WalkForwardSolverListener(
                killedTransitionTarget,
                new SingleNode<Node<Statement, Val>>(
                    new Node<>(fieldWriteStatement, fieldWriteStatement.getRightOp())),
                0));
  }

  private class WalkForwardSolverListener
      extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

    private INode<Node<Statement, Val>> stateInBwSolver;
    private int walkDepth;

    public WalkForwardSolverListener(
        INode<Node<Statement, Val>> target,
        INode<Node<Statement, Val>> stateInBwSolver,
        int walkDepth) {
      super(target);
      this.stateInBwSolver = stateInBwSolver;
      this.walkDepth = walkDepth;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
      if (t.getLabel().equals(Field.empty())) {
        if (forwardSolver.getFieldAutomaton().isUnbalancedState(t.getTarget())) {
          if (t.getStart().equals(CopyAccessPathChain.this.killedTransitionTarget)) {
            // Do a simple backwardSolve(...)...
            BackwardQuery query =
                BackwardQuery.make(fieldWriteStatement, fieldWriteStatement.getRightOp());
            INode<Node<Statement, Val>> fieldTarget = backwardSolver.createQueryNodeField(query);
            INode<Val> callTarget =
                backwardSolver.generateCallState(new SingleNode<>(query.var()), query.stmt());
            backwardSolver.solve(
                query.asNode(), Field.empty(), fieldTarget, query.stmt(), callTarget);
            return;
          }
          // addReachable(stateInBwSolver);
        }
        return;
      }
      INode<Node<Statement, Val>> targetState =
          backwardSolver.generateFieldState(
              new SingleNode<>(new Node<>(Statement.epsilon(), Val.zero())), t.getLabel());
      Transition<Field, INode<Node<Statement, Val>>> insert =
          new Transition<Field, INode<Node<Statement, Val>>>(
              stateInBwSolver, t.getLabel(), targetState);
      queueOrAdd(insert);
      int newDepth = walkDepth + 1;
      if (MAX_WALK_DEPTH < 0 || newDepth < MAX_WALK_DEPTH) {
        forwardSolver
            .getFieldAutomaton()
            .registerListener(new WalkForwardSolverListener(t.getTarget(), targetState, newDepth));
      }
    }

    @Override
    public void onInTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {}

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      WalkForwardSolverListener that = (WalkForwardSolverListener) o;
      if (!getEnclosingInstance().equals(that.getEnclosingInstance())) return false;
      return Objects.equal(stateInBwSolver, that.stateInBwSolver);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getEnclosingInstance().hashCode();
      result = prime * result + ((stateInBwSolver == null) ? 0 : stateInBwSolver.hashCode());
      return result;
    }

    private CopyAccessPathChain getEnclosingInstance() {
      return CopyAccessPathChain.this;
    }
  }

  // Copied from ExecuteImportFielStmtPOI

  private Set<INode<Node<Statement, Val>>> reachable = Sets.newHashSet();
  private Multimap<INode<Node<Statement, Val>>, InsertFieldTransitionCallback> delayedTransitions =
      HashMultimap.create();

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
      backwardSolver.getFieldAutomaton().addTransition(transToInsert);
    } else {
      delayedTransitions.put(
          transToInsert.getTarget(), new InsertFieldTransitionCallback(transToInsert));
    }
  }

  private class InsertFieldTransitionCallback {
    private final Transition<Field, INode<Node<Statement, Val>>> trans;

    public InsertFieldTransitionCallback(Transition<Field, INode<Node<Statement, Val>>> trans) {
      this.trans = trans;
    }

    public void trigger() {
      backwardSolver.getFieldAutomaton().addTransition(trans);
      addReachable(trans.getStart());
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getEnclosingInstance().hashCode();
      result = prime * result + ((trans == null) ? 0 : trans.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      InsertFieldTransitionCallback other = (InsertFieldTransitionCallback) obj;
      if (!getEnclosingInstance().equals(other.getEnclosingInstance())) return false;
      if (trans == null) {
        if (other.trans != null) return false;
      } else if (!trans.equals(other.trans)) return false;
      return true;
    }

    private CopyAccessPathChain getEnclosingInstance() {
      return CopyAccessPathChain.this;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((backwardSolver == null) ? 0 : backwardSolver.hashCode());
    result = prime * result + ((fieldWriteStatement == null) ? 0 : fieldWriteStatement.hashCode());
    result = prime * result + ((forwardSolver == null) ? 0 : forwardSolver.hashCode());
    result =
        prime * result + ((killedTransitionTarget == null) ? 0 : killedTransitionTarget.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    CopyAccessPathChain other = (CopyAccessPathChain) obj;
    if (backwardSolver == null) {
      if (other.backwardSolver != null) return false;
    } else if (!backwardSolver.equals(other.backwardSolver)) return false;
    if (fieldWriteStatement == null) {
      if (other.fieldWriteStatement != null) return false;
    } else if (!fieldWriteStatement.equals(other.fieldWriteStatement)) return false;
    if (forwardSolver == null) {
      if (other.forwardSolver != null) return false;
    } else if (!forwardSolver.equals(other.forwardSolver)) return false;
    if (killedTransitionTarget == null) {
      if (other.killedTransitionTarget != null) return false;
    } else if (!killedTransitionTarget.equals(other.killedTransitionTarget)) return false;
    return true;
  }
}
