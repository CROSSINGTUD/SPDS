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

import boomerang.BackwardQuery;
import boomerang.BoomerangOptions;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.controlflowgraph.ObservableControlFlowGraph;
import boomerang.scene.AllocVal;
import boomerang.scene.CallSiteStatement;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Field;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Pair;
import boomerang.scene.ReturnSiteStatement;
import boomerang.scene.Statement;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Type;
import boomerang.scene.Val;
import com.google.common.collect.Sets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
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
import wpds.interfaces.State;

public abstract class BackwardBoomerangSolver<W extends Weight> extends AbstractBoomerangSolver<W> {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackwardBoomerangSolver.class);
  private final BackwardQuery query;

  public BackwardBoomerangSolver(
      ObservableICFG<Statement, Method> icfg,
      ObservableControlFlowGraph cfg,
      Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> genField,
      BackwardQuery query,
      BoomerangOptions options,
      NestedWeightedPAutomatons<Statement, INode<Val>, W> callSummaries,
      NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> fieldSummaries,
      DataFlowScope scope,
      Strategies strategies,
      Type propagationType) {
    super(
        icfg,
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

  @Override
  protected boolean killFlow(Method m, Statement curr, Val value) {
    if (value.isStatic()) return false;
    if (!m.getLocals().contains(value)) return true;
    return false;
  }

  public INode<Node<Statement, Val>> generateFieldState(
      final INode<Node<Statement, Val>> d, final Field loc) {
    Entry<INode<Node<Statement, Val>>, Field> e = new SimpleEntry<>(d, loc);
    if (!generatedFieldState.containsKey(e)) {
      generatedFieldState.put(
          e,
          new GeneratedState<>(new SingleNode<>(new Node<>(Statement.epsilon(), Val.zero())), loc));
    }
    return generatedFieldState.get(e);
  }

  /*
  @Override
  public INode<Val> generateCallState(INode<Val> d, Statement loc) {
    Entry<INode<Val>, Statement> e = new AbstractMap.SimpleEntry<>(d, loc);
    if (!generatedCallState.containsKey(e)) {
      generatedCallState.put(
          e, new GeneratedState<Val, Statement>(new SingleNode<Val>(Val.zero()), loc));
    }
    return generatedCallState.get(e);
  }
  */

  @Override
  protected Collection<? extends State> computeReturnFlow(
      Method method, Statement callerReturnStatement, Val value) {
    Set<State> out = Sets.newHashSet();
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
      out.add(new PopNode<>(value, PDSSystem.CALLS));
    }
    return out;
  }

  protected void callFlow(Method caller, Node<Statement, Val> curr, CallSiteStatement callSite) {
    icfg.addCalleeListener(new CallSiteCalleeListener(curr, caller, callSite));
    InvokeExpr invokeExpr = callSite.getInvokeExpr();
    if (dataFlowScope.isExcluded(invokeExpr.getMethod())) {
      byPassFlowAtCallsite(caller, curr);
    }
  }

  private void byPassFlowAtCallsite(Method caller, Node<Statement, Val> curr) {
    normalFlow(caller, curr);
    for (Statement returnSite :
        curr.stmt().getMethod().getControlFlowGraph().getPredsOf(curr.stmt())) {

      for (State s : getEmptyCalleeFlow(caller, curr.stmt(), curr.fact(), returnSite)) {
        propagate(curr, s);
      }
    }
  }

  @Override
  public void computeSuccessor(Node<Statement, Val> node) {
    Statement stmt = node.stmt();
    LOGGER.trace("BW: Computing successor of {} for {}", node, this);
    Val value = node.fact();
    assert !(value instanceof AllocVal);
    Method method = stmt.getMethod();
    if (method == null) return;
    //        if(dataFlowScope.isExcluded(method))
    //          return;
    if (killFlow(method, stmt, value)) {
      return;
    }
    if (isReturnSiteStatement(stmt, value) && INTERPROCEDURAL) {
      ReturnSiteStatement returnSiteStatement = (ReturnSiteStatement) stmt;
      CallSiteStatement callSite = returnSiteStatement.getCallSiteStatement();
      callFlow(method, node, callSite);
    } else if (icfg.isExitStmt(stmt)) {
      returnFlow(method, node);
    } else {
      normalFlow(method, node);
    }
  }

  protected boolean isReturnSiteStatement(Statement stmt, Val value) {
    if ((stmt instanceof ReturnSiteStatement)) {
      ReturnSiteStatement returnSiteStatement = (ReturnSiteStatement) stmt;
      CallSiteStatement callSite = returnSiteStatement.getCallSiteStatement();
      return callSite.uses(value);
    }
    return false;
  }

  protected void normalFlow(Method method, Node<Statement, Val> currNode) {
    Statement curr = currNode.stmt();
    Val value = currNode.fact();
    for (Statement succ : curr.getMethod().getControlFlowGraph().getPredsOf(curr)) {
      Collection<State> flow = computeNormalFlow(method, curr, value, succ);
      for (State s : flow) {
        propagate(currNode, s);
      }
    }
  }

  protected Collection<? extends State> computeCallFlow(
      CallSiteStatement callSite,
      InvokeExpr invokeExpr,
      Val fact,
      Method callee,
      Statement calleeSp) {
    if (calleeSp.isThrowStmt()) {
      return Collections.emptySet();
    }
    Set<State> out = Sets.newHashSet();
    if (invokeExpr.isInstanceInvokeExpr()) {
      if (invokeExpr.getBase().equals(fact) && !callee.isStatic()) {
        out.add(
            new PushNode<Statement, Val, Statement>(
                calleeSp, callee.getThisLocal(), callSite, PDSSystem.CALLS));
      }
    }
    List<Val> parameterLocals = callee.getParameterLocals();
    int i = 0;
    for (Val arg : invokeExpr.getArgs()) {
      if (arg.equals(fact) && parameterLocals.size() > i) {
        Val param = parameterLocals.get(i);
        out.add(
            new PushNode<Statement, Val, Statement>(calleeSp, param, callSite, PDSSystem.CALLS));
      }
      i++;
    }

    if (callSite.isAssign() && calleeSp.isReturnStmt()) {
      if (callSite.getLeftOp().equals(fact)) {
        out.add(
            new PushNode<Statement, Val, Statement>(
                calleeSp, calleeSp.getReturnOp(), callSite, PDSSystem.CALLS));
      }
    }
    if (fact.isStatic()) {
      out.add(
          new PushNode<Statement, Val, Statement>(
              calleeSp, fact.withNewMethod(callee), callSite, PDSSystem.CALLS));
    }
    return out;
  }

  @Override
  protected Collection<State> computeNormalFlow(
      Method method, Statement curr, Val fact, Statement succ) {
    if (options.getAllocationVal(method, curr, fact, icfg).isPresent()) {
      return Collections.emptySet();
    }
    if (curr.isThrowStmt()) {
      return Collections.emptySet();
    }
    Set<State> out = Sets.newHashSet();

    boolean leftSideMatches = false;
    if (curr.isAssign()) {
      Val leftOp = curr.getLeftOp();
      Val rightOp = curr.getRightOp();
      if (leftOp.equals(fact)) {
        leftSideMatches = true;
        if (curr.isFieldLoad()) {
          if (options.trackFields()) {
            Pair<Val, Field> ifr = curr.getFieldLoad();
            if (!options.ignoreInnerClassFields() || !ifr.getY().isInnerClassField()) {
              out.add(new PushNode<>(succ, ifr.getX(), ifr.getY(), PDSSystem.FIELDS));
            }
          }
        } else if (curr.isStaticFieldLoad()) {
          if (options.trackFields()) {
            strategies
                .getStaticFieldStrategy()
                .handleBackward(curr, curr.getLeftOp(), curr.getStaticField(), succ, out, this);
          }
        } else if (rightOp.isArrayRef()) {
          Pair<Val, Integer> arrayBase = curr.getArrayBase();
          if (options.trackFields()) {
            strategies.getArrayHandlingStrategy().handleBackward(curr, arrayBase, succ, out, this);
          }
          // leftSideMatches = false;
        } else if (rightOp.isCast()) {
          out.add(new Node<>(succ, rightOp.getCastOp()));
        } else if (curr.isPhiStatement()) {
          Collection<Val> phiVals = curr.getPhiVals();
          for (Val v : phiVals) {
            out.add(new Node<>(succ, v));
          }
        } else {
          if (curr.isFieldLoadWithBase(fact)) {
            out.add(new ExclusionNode<>(succ, fact, curr.getLoadedField()));
          } else {
            out.add(new Node<>(succ, rightOp));
          }
        }
      }
      if (curr.isFieldStore()) {
        Pair<Val, Field> ifr = curr.getFieldStore();
        Val base = ifr.getX();
        if (base.equals(fact)) {
          NodeWithLocation<Statement, Val, Field> succNode =
              new NodeWithLocation<>(succ, rightOp, ifr.getY());
          out.add(new PopNode<>(succNode, PDSSystem.FIELDS));
        }
      } else if (curr.isStaticFieldStore()) {
        StaticFieldVal staticField = curr.getStaticField();
        if (fact.isStatic() && fact.equals(staticField)) {
          out.add(new Node<>(succ, rightOp));
        }
      } else if (leftOp.isArrayRef()) {
        Pair<Val, Integer> arrayBase = curr.getArrayBase();
        if (arrayBase.getX().equals(fact)) {
          NodeWithLocation<Statement, Val, Field> succNode =
              new NodeWithLocation<>(succ, rightOp, Field.array(arrayBase.getY()));
          out.add(new PopNode<>(succNode, PDSSystem.FIELDS));
        }
      }
    }
    if (!leftSideMatches) out.add(new Node<>(succ, fact));
    return out;
  }

  @Override
  public void applyCallSummary(
      Statement callSite,
      Val factAtSpInCallee,
      Statement spInCallee,
      Statement exitStmt,
      Val exitingFact) {
    Set<Node<Statement, Val>> out = Sets.newHashSet();
    if (callSite.containsInvokeExpr()) {
      if (exitingFact.isThisLocal()) {
        if (callSite.getInvokeExpr().isInstanceInvokeExpr()) {
          out.add(new Node<>(callSite, callSite.getInvokeExpr().getBase()));
        }
      }
      if (exitingFact.isReturnLocal()) {
        if (callSite.isAssign()) {
          out.add(new Node<>(callSite, callSite.getLeftOp()));
        }
      }
      for (int i = 0; i < callSite.getInvokeExpr().getArgs().size(); i++) {
        if (exitingFact.isParameterLocal(i)) {
          out.add(new Node<>(callSite, callSite.getInvokeExpr().getArg(i)));
        }
      }
    }
    for (Node<Statement, Val> xs : out) {
      addNormalCallFlow(new Node<>(callSite, exitingFact), xs);
      addNormalFieldFlow(new Node<>(exitStmt, exitingFact), xs);
    }
  }

  @Override
  protected void propagateUnbalancedToCallSite(
      CallSiteStatement callSite, Transition<Statement, INode<Val>> transInCallee) {
    GeneratedState<Val, Statement> target =
        (GeneratedState<Val, Statement>) transInCallee.getTarget();
    Node<Statement, Val> curr = new Node<>(callSite.getReturnSiteStatement(), query.var());

    Transition<Statement, INode<Val>> callTrans =
        new Transition<>(
            wrap(curr.fact()), curr.stmt(), generateCallState(wrap(curr.fact()), curr.stmt()));
    callAutomaton.addTransition(callTrans);
    callAutomaton.addUnbalancedState(generateCallState(wrap(curr.fact()), curr.stmt()), target);

    State s = new PushNode<>(target.location(), target.node().fact(), callSite, PDSSystem.CALLS);
    propagate(curr, s);
  }

  private final class CallSiteCalleeListener implements CalleeListener<Statement, Method> {
    private final CallSiteStatement callSite;
    private final Node<Statement, Val> curr;
    private final Method caller;

    private CallSiteCalleeListener(
        Node<Statement, Val> curr, Method caller, CallSiteStatement callSite) {
      this.curr = curr;
      this.callSite = callSite;
      this.caller = caller;
      if (!curr.stmt().equals(callSite.getReturnSiteStatement()))
        throw new RuntimeException("Mismatch of call and return sites!");
    }

    @Override
    public Statement getObservedCaller() {
      return callSite;
    }

    @Override
    public void onCalleeAdded(Statement callSite, Method callee) {
      if (callee.isStaticInitializer()) {
        return;
      }
      InvokeExpr invokeExpr = callSite.getInvokeExpr();
      for (Statement calleeSp : icfg.getStartPointsOf(callee)) {
        Collection<? extends State> res =
            computeCallFlow(
                (CallSiteStatement) callSite, invokeExpr, curr.fact(), callee, calleeSp);
        for (State o : res) {
          BackwardBoomerangSolver.this.propagate(curr, o);
        }
      }
    }

    @Override
    public void onNoCalleeFound() {
      byPassFlowAtCallsite(caller, curr);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((caller == null) ? 0 : caller.hashCode());
      result = prime * result + ((curr == null) ? 0 : curr.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      CallSiteCalleeListener other = (CallSiteCalleeListener) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (caller == null) {
        if (other.caller != null) return false;
      } else if (!caller.equals(other.caller)) return false;
      if (curr == null) {
        if (other.curr != null) return false;
      } else if (!curr.equals(other.curr)) return false;
      return true;
    }

    private BackwardBoomerangSolver getOuterType() {
      return BackwardBoomerangSolver.this;
    }
  }

  @Override
  public String toString() {
    return "BackwardBoomerangSolver{" + "query=" + query + '}';
  }
}
