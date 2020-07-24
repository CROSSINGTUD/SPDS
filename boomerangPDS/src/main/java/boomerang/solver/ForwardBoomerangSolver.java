/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.solver;

import boomerang.BoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.controlflowgraph.ObservableControlFlowGraph;
import boomerang.controlflowgraph.SuccessorListener;
import boomerang.results.NullPointerDereference;
import boomerang.scene.AllocVal;
import boomerang.scene.CallSiteStatement;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Field;
import boomerang.scene.IfStatement;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Pair;
import boomerang.scene.ReturnSiteStatement;
import boomerang.scene.Statement;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Type;
import boomerang.scene.Val;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.LoggerFactory;
import sync.pds.solver.nodes.ExclusionNode;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.NestedWeightedPAutomatons;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;

public abstract class ForwardBoomerangSolver<W extends Weight> extends AbstractBoomerangSolver<W> {
  private static final org.slf4j.Logger LOGGER =
      LoggerFactory.getLogger(ForwardBoomerangSolver.class);
  private final ForwardQuery query;

  public ForwardBoomerangSolver(
      ObservableICFG<Statement, Method> callGraph,
      ObservableControlFlowGraph cfg,
      ForwardQuery query,
      Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> genField,
      BoomerangOptions options,
      NestedWeightedPAutomatons<Statement, INode<Val>, W> callSummaries,
      NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> fieldSummaries,
      DataFlowScope scope,
      Strategies strategies,
      Type propagationType) {
    super(
        callGraph,
        cfg,
        genField,
        options,
        callSummaries,
        fieldSummaries,
        scope,
        strategies,
        propagationType);
    this.query = query;
  }

  private final class OverwriteAtFieldStore
      extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {
    private final Statement nextStmt;

    private OverwriteAtFieldStore(INode<Node<Statement, Val>> state, Statement nextStmt) {
      super(state);
      this.nextStmt = nextStmt;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
      if (t.getLabel().equals(nextStmt.getFieldStore().getY())) {
        LOGGER.trace("Overwriting field {} at {}", t.getLabel(), nextStmt);
        overwriteFieldAtStatement(nextStmt, t);
      }
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
      result = prime * result + getEnclosingInstance().hashCode();
      result = prime * result + ((nextStmt == null) ? 0 : nextStmt.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      OverwriteAtFieldStore other = (OverwriteAtFieldStore) obj;
      if (!getEnclosingInstance().equals(other.getEnclosingInstance())) return false;
      if (nextStmt == null) {
        if (other.nextStmt != null) return false;
      } else if (!nextStmt.equals(other.nextStmt)) return false;
      return true;
    }

    private ForwardBoomerangSolver getEnclosingInstance() {
      return ForwardBoomerangSolver.this;
    }
  }

  private final class OverwriteAtArrayStore
      extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {
    private final Statement nextStmt;

    private OverwriteAtArrayStore(INode<Node<Statement, Val>> state, Statement nextStmt) {
      super(state);
      this.nextStmt = nextStmt;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<Statement, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
      if (t.getLabel().equals(Field.array(nextStmt.getArrayBase().getY()))) {
        LOGGER.trace("Overwriting field {} at {}", t.getLabel(), nextStmt);
        overwriteFieldAtStatement(nextStmt, t);
      }
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
      result = prime * result + getEnclosingInstance().hashCode();
      result = prime * result + ((nextStmt == null) ? 0 : nextStmt.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      OverwriteAtArrayStore other = (OverwriteAtArrayStore) obj;
      if (!getEnclosingInstance().equals(other.getEnclosingInstance())) return false;
      if (nextStmt == null) {
        if (other.nextStmt != null) return false;
      } else if (!nextStmt.equals(other.nextStmt)) return false;
      return true;
    }

    private ForwardBoomerangSolver getEnclosingInstance() {
      return ForwardBoomerangSolver.this;
    }
  }

  @Override
  protected void propagateUnbalancedToCallSite(
      CallSiteStatement callSite, Transition<Statement, INode<Val>> transInCallee) {
    GeneratedState<Val, Statement> target =
        (GeneratedState<Val, Statement>) transInCallee.getTarget();
    Node<Statement, Val> curr = new Node<>(callSite, query.var());
    /**
     * Transition<Field, INode<Node<Statement, Val>>> fieldTrans = new Transition<>(new
     * SingleNode<>(curr), emptyField(), new SingleNode<>(curr));
     * fieldAutomaton.addTransition(fieldTrans);*
     */
    Transition<Statement, INode<Val>> callTrans =
        new Transition<>(
            wrap(curr.fact()), curr.stmt(), generateCallState(wrap(curr.fact()), curr.stmt()));
    callAutomaton.addTransition(callTrans);
    callAutomaton.addUnbalancedState(generateCallState(wrap(curr.fact()), curr.stmt()), target);
    State s =
        new PushNode<>(
            target.location(),
            target.node().fact(),
            callSite.getReturnSiteStatement(),
            PDSSystem.CALLS);
    propagate(curr, s);
  }

  private final class CallSiteCalleeListener implements CalleeListener<Statement, Method> {
    private final Method caller;
    private final Statement callSite;
    private final Node<Statement, Val> currNode;
    private final InvokeExpr invokeExpr;

    private CallSiteCalleeListener(
        Method caller, Statement callSite, Node<Statement, Val> currNode, InvokeExpr invokeExpr) {
      this.caller = caller;
      this.callSite = callSite;
      this.currNode = currNode;
      this.invokeExpr = invokeExpr;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
      result = prime * result + ((caller == null) ? 0 : caller.hashCode());
      result = prime * result + ((currNode == null) ? 0 : currNode.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      CallSiteCalleeListener other = (CallSiteCalleeListener) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (callSite == null) {
        if (other.callSite != null) return false;
      } else if (!callSite.equals(other.callSite)) return false;
      if (caller == null) {
        if (other.caller != null) return false;
      } else if (!caller.equals(other.caller)) return false;
      if (currNode == null) {
        if (other.currNode != null) return false;
      } else if (!currNode.equals(other.currNode)) return false;
      return true;
    }

    @Override
    public void onCalleeAdded(Statement callSite, Method callee) {
      if (callee.isStaticInitializer()) {
        return;
      }
      LOGGER.trace(
          "Call-flow of {} at callsite: {} to callee method: {} for {}",
          currNode.fact(),
          callSite,
          callee,
          this);
      for (Statement calleeSp : icfg.getStartPointsOf(callee)) {
        Collection<? extends State> res =
            computeCallFlow(
                caller,
                (CallSiteStatement) callSite,
                ((CallSiteStatement) callSite).getReturnSiteStatement(),
                invokeExpr,
                currNode,
                callee,
                calleeSp);
        for (State s : res) {
          propagate(currNode, s);
        }
      }
    }

    @Override
    public void onNoCalleeFound() {
      byPassFlowAtCallSite(caller, currNode, callSite);
    }

    @Override
    public Statement getObservedCaller() {
      return callSite;
    }

    private ForwardBoomerangSolver getOuterType() {
      return ForwardBoomerangSolver.this;
    }
  }

  @Override
  public void applyCallSummary(
      Statement returnSiteStatement,
      Val factInCallee,
      Statement spInCallee,
      Statement returnSite,
      Val returnedFact) {
    CallSiteStatement callSite = ((ReturnSiteStatement) returnSiteStatement).getCallSiteStatement();

    Set<Node<Statement, Val>> out = Sets.newHashSet();
    if (callSite.containsInvokeExpr()) {
      if (returnedFact.isThisLocal()) {
        if (callSite.getInvokeExpr().isInstanceInvokeExpr()) {
          out.add(new Node<>(returnSiteStatement, callSite.getInvokeExpr().getBase()));
        }
      }
      if (returnedFact.isReturnLocal()) {
        if (callSite.isAssign()) {
          out.add(new Node<>(returnSiteStatement, callSite.getLeftOp()));
        }
      }
      for (int i = 0; i < callSite.getInvokeExpr().getArgs().size(); i++) {
        if (returnedFact.isParameterLocal(i)) {
          out.add(new Node<>(returnSiteStatement, callSite.getInvokeExpr().getArg(i)));
        }
      }
    }
    if (returnedFact.isStatic()) {
      out.add(
          new Node<>(
              returnSiteStatement, returnedFact.withNewMethod(returnSiteStatement.getMethod())));
    }
    for (Node<Statement, Val> xs : out) {
      addNormalCallFlow(new Node<>(returnSiteStatement, returnedFact), xs);
      addNormalFieldFlow(new Node<>(returnSite, returnedFact), xs);
    }
  }

  public Collection<? extends State> computeCallFlow(
      Method caller,
      CallSiteStatement callSite,
      ReturnSiteStatement returnSiteStatement,
      InvokeExpr invokeExpr,
      Node<Statement, Val> currNode,
      Method callee,
      Statement calleeSp) {
    Val fact = currNode.fact();
    if (callee.isStaticInitializer()) {
      return Collections.emptySet();
    }
    if (dataFlowScope.isExcluded(callee)) {
      byPassFlowAtCallSite(caller, currNode, callSite);
      return Collections.emptySet();
    }
    Set<State> out = Sets.newHashSet();
    if (invokeExpr.isInstanceInvokeExpr()) {
      if (invokeExpr.getBase().equals(fact) && !callee.isStatic()) {
        out.add(
            new PushNode<Statement, Val, Statement>(
                calleeSp, callee.getThisLocal(), returnSiteStatement, PDSSystem.CALLS));
      }
    }
    int i = 0;
    List<Val> parameterLocals = callee.getParameterLocals();
    for (Val arg : invokeExpr.getArgs()) {
      if (arg.equals(fact) && parameterLocals.size() > i) {
        Val param = parameterLocals.get(i);
        out.add(
            new PushNode<Statement, Val, Statement>(
                calleeSp, param, returnSiteStatement, PDSSystem.CALLS));
      }
      i++;
    }
    if (fact.isStatic()) {
      out.add(
          new PushNode<Statement, Val, Statement>(
              calleeSp, fact.withNewMethod(callee), returnSiteStatement, PDSSystem.CALLS));
    }
    return out;
  }

  public Query getQuery() {
    return query;
  }

  @Override
  protected boolean killFlow(Method m, Statement curr, Val value) {
    if (!m.getLocals().contains(value) && !value.isStatic()) return true;
    if (curr.isThrowStmt() || curr.isCatchStmt()) {
      return true;
    }

    if (curr.isAssign()) {
      // Kill x at any statement x = * during propagation.
      if (curr.getLeftOp().equals(value)) {
        // But not for a statement x = x.f
        if (curr.isFieldLoad()) {
          Pair<Val, Field> ifr = curr.getFieldLoad();
          if (ifr.getX().equals(value)) {
            return false;
          }
        }
        return true;
      }
      if (curr.isStaticFieldStore()) {
        StaticFieldVal sf = curr.getStaticField();
        if (value.isStatic() && value.equals(sf)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void computeSuccessor(Node<Statement, Val> node) {
    Statement curr = node.stmt();
    Val value = node.fact();
    assert !(value instanceof AllocVal);
    Method method = curr.getMethod();
    if (method == null) return;
    if (dataFlowScope.isExcluded(method)) {
      return;
    }
    if (icfg.isExitStmt(curr)) {
      returnFlow(method, node);
      return;
    }
    cfg.addSuccsOfListener(
        new SuccessorListener(curr) {

          @Override
          public void getSuccessor(Statement succ) {
            if (query.getType().isNullType() && curr.isIfStmt() && curr.killAtIfStmt(value, succ)) {
              return;
            }

            if (succ.containsInvokeExpr() && (succ.isParameter(value) || value.isStatic())) {
              callFlow(method, node, (CallSiteStatement) succ, succ.getInvokeExpr());
            } else if (!killFlow(method, succ, value)) {
              checkForFieldOverwrite(method, curr, succ, value);
              Collection<State> out = computeNormalFlow(method, curr, value, succ);
              for (State s : out) {
                LOGGER.trace("{}: {} -> {}", s, node, ForwardBoomerangSolver.this.query);
                propagate(node, s);
              }
            }
          }
        });
  }

  private void checkForFieldOverwrite(
      Method method, Statement curr, Statement nextStmt, Val value) {
    if ((nextStmt.isFieldStore() && nextStmt.getFieldStore().getX().equals(value))) {
      Node<Statement, Val> node = new Node<>(curr, value);
      fieldAutomaton.registerListener(new OverwriteAtFieldStore(new SingleNode<>(node), nextStmt));
    } else if ((nextStmt.isArrayStore() && nextStmt.getArrayBase().getX().equals(value))) {
      Node<Statement, Val> node = new Node<>(curr, value);
      fieldAutomaton.registerListener(new OverwriteAtArrayStore(new SingleNode<>(node), nextStmt));
    }
  }

  protected abstract void overwriteFieldAtStatement(
      Statement fieldWriteStatement,
      Transition<Field, INode<Node<Statement, Val>>> killedTransition);

  @Override
  public Collection<State> computeNormalFlow(
      Method method, Statement curr, Val fact, Statement succ) {
    Set<State> out = Sets.newHashSet();
    if (!succ.isFieldWriteWithBase(fact)) {
      // always maintain data-flow if not a field write // killFlow has
      // been taken care of
      if (!options.trackReturnOfInstanceOf()
          || !(query.getType().isNullType() && succ.isInstanceOfStatement(fact))) {
        out.add(new Node<>(succ, fact));
      }
    } else {
      out.add(new ExclusionNode<>(succ, fact, succ.getWrittenField()));
    }
    if (succ.isAssign()) {
      Val leftOp = succ.getLeftOp();
      Val rightOp = succ.getRightOp();
      if (rightOp.equals(fact)) {
        if (succ.isFieldStore()) {
          Pair<Val, Field> ifr = succ.getFieldStore();
          if (options.trackFields()) {
            if (!options.ignoreInnerClassFields() || !ifr.getY().isInnerClassField()) {
              out.add(new PushNode<>(succ, ifr.getX(), ifr.getY(), PDSSystem.FIELDS));
            }
          }
        } else if (succ.isStaticFieldStore()) {
          StaticFieldVal sf = succ.getStaticField();
          if (options.trackFields()) {
            strategies.getStaticFieldStrategy().handleForward(succ, rightOp, sf, out, this);
          }
        } else if (leftOp.isArrayRef()) {
          Pair<Val, Integer> arrayBase = succ.getArrayBase();
          if (options.trackFields()) {
            strategies.getArrayHandlingStrategy().handleForward(succ, arrayBase, out, this);
          }
        } else {
          out.add(new Node<>(succ, leftOp));
        }
      }
      if (succ.isFieldLoad()) {
        Pair<Val, Field> ifr = succ.getFieldLoad();
        if (ifr.getX().equals(fact)) {
          NodeWithLocation<Statement, Val, Field> succNode =
              new NodeWithLocation<>(succ, leftOp, ifr.getY());
          out.add(new PopNode<>(succNode, PDSSystem.FIELDS));
        }
      } else if (succ.isStaticFieldLoad()) {
        StaticFieldVal sf = succ.getStaticField();
        if (fact.isStatic() && fact.equals(sf)) {
          out.add(new Node<>(succ, leftOp));
        }
      } else if (rightOp.isArrayRef()) {
        Pair<Val, Integer> arrayBase = succ.getArrayBase();
        if (arrayBase.getX().equals(fact)) {
          NodeWithLocation<Statement, Val, Field> succNode =
              new NodeWithLocation<>(succ, leftOp, Field.array(arrayBase.getY()));

          //                    out.add(new Node<Statement, Val>(succ, leftOp));
          out.add(new PopNode<>(succNode, PDSSystem.FIELDS));
        }
      } else if (rightOp.isCast()) {
        if (rightOp.getCastOp().equals(fact)) {
          out.add(new Node<>(succ, leftOp));
        }
      } else if (rightOp.isInstanceOfExpr()
          && query.getType().isNullType()
          && options.trackReturnOfInstanceOf()) {
        if (rightOp.getInstanceOfOp().equals(fact)) {
          out.add(new Node<>(succ, fact.withSecondVal(leftOp)));
        }
      } else if (succ.isPhiStatement()) {
        Collection<Val> phiVals = succ.getPhiVals();
        if (phiVals.contains(fact)) {
          out.add(new Node<>(succ, succ.getLeftOp()));
        }
      }
    }

    return out;
  }

  protected void callFlow(
      Method caller,
      Node<Statement, Val> currNode,
      CallSiteStatement callSite,
      InvokeExpr invokeExpr) {
    assert icfg.isCallStmt(callSite);
    if (dataFlowScope.isExcluded(invokeExpr.getMethod())) {
      byPassFlowAtCallSite(caller, currNode, callSite);
    }

    icfg.addCalleeListener(new CallSiteCalleeListener(caller, callSite, currNode, invokeExpr));
  }

  private void byPassFlowAtCallSite(
      Method caller, Node<Statement, Val> currNode, Statement callSite) {
    LOGGER.trace(
        "Bypassing call flow of {} at callsite: {} for {}", currNode.fact(), callSite, this);
    for (State s : computeNormalFlow(caller, currNode.stmt(), currNode.fact(), callSite)) {
      propagate(currNode, s);
    }
    cfg.addSuccsOfListener(
        new SuccessorListener(currNode.stmt()) {

          @Override
          public void getSuccessor(Statement returnSite) {
            for (State s : getEmptyCalleeFlow(caller, callSite, currNode.fact(), returnSite)) {
              propagate(currNode, s);
            }
          }
        });
  }

  @Override
  public Collection<? extends State> computeReturnFlow(Method method, Statement curr, Val value) {
    if (curr.isThrowStmt() && !options.throwFlows()) {
      return Collections.emptySet();
    }
    Set<State> out = Sets.newHashSet();
    if (curr.isReturnStmt()) {
      if (curr.getReturnOp().equals(value)) {
        out.add(new PopNode<>(value, PDSSystem.CALLS));
      }
    }
    if (!method.isStatic()) {
      if (method.getThisLocal().equals(value)) {
        out.add(new PopNode<>(value, PDSSystem.CALLS));
      }
    }
    for (Val param : method.getParameterLocals()) {
      if (param.equals(value)) {
        out.add(new PopNode<>(value, PDSSystem.CALLS));
      }
    }
    if (value.isStatic()) {
      // TODO value.withNewMethod(callSite.getMethod()) must be done when applying summary
      out.add(new PopNode<>(value, PDSSystem.CALLS));
    }
    return out;
  }

  public ControlFlowLatticeElement controlFlowStep(
      Statement curr, Statement succ, Collection<Statement> succs) {
    if (query.getType().isNullType() && curr.isIfStmt()) {
      boolean successorIsTarget = false;
      IfStatement ifStmt = curr.getIfStmt();
      if (succ instanceof CallSiteStatement) {
        CallSiteStatement callSiteStatement = (CallSiteStatement) succ;
        if (ifStmt.getTarget().equals(callSiteStatement)) {
          successorIsTarget = true;
        }
      } else if (ifStmt.getTarget().equals(succ)) {
        successorIsTarget = true;
      }
      for (Transition<Field, INode<Node<Statement, Val>>> t :
          perStatementFieldTransitions.get(curr)) {
        if (!t.getLabel().equals(Field.empty()) || t.getStart() instanceof GeneratedState) {
          continue;
        }
        Node<Statement, Val> node = t.getStart().fact();
        Val fact = node.fact();
        switch (ifStmt.evaluate(fact)) {
          case TRUE:
            if (!successorIsTarget) {
              return KillElement.create();
            }
            break;
          case FALSE:
            if (successorIsTarget) {
              return KillElement.create();
            }
        }
      }
    }
    if (query.getType().isNullType()) {
      for (Transition<Field, INode<Node<Statement, Val>>> t :
          perStatementFieldTransitions.get(curr)) {
        if (t.getLabel().equals(Field.empty())
            && NullPointerDereference.isNullPointerNode(t.getStart().fact()))
          return KillElement.create();
      }
    }
    return UnknownElement.create();
  }

  @Override
  public String toString() {
    return "ForwardSolver: " + query;
  }

  public interface ControlFlowLatticeElement {
    ControlFlowLatticeElement merge(ControlFlowLatticeElement other);
  }

  public static class KillElement implements ControlFlowLatticeElement {
    private KillElement() {}

    public static KillElement create() {
      return new KillElement();
    }

    @Override
    public ControlFlowLatticeElement merge(ControlFlowLatticeElement other) {
      if (other instanceof ContinueElement) return other;
      return this;
    }
  }

  public static class ContinueElement implements ControlFlowLatticeElement {
    private ContinueElement() {}

    public static ContinueElement create() {
      return new ContinueElement();
    }

    @Override
    public ControlFlowLatticeElement merge(ControlFlowLatticeElement other) {
      return this;
    }
  }

  public static class UnknownElement implements ControlFlowLatticeElement {
    private UnknownElement() {}

    public static UnknownElement create() {
      return new UnknownElement();
    }

    @Override
    public ControlFlowLatticeElement merge(ControlFlowLatticeElement other) {
      return other;
    }
  }
}
