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
import boomerang.controlflowgraph.PredecessorListener;
import boomerang.controlflowgraph.SuccessorListener;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Field;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Pair;
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
import wpds.interfaces.Location;
import wpds.interfaces.State;

public abstract class BackwardBoomerangSolver<W extends Weight> extends AbstractBoomerangSolver<W> {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackwardBoomerangSolver.class);
  private final BackwardQuery query;

  public BackwardBoomerangSolver(
      ObservableICFG<Statement, Method> icfg,
      ObservableControlFlowGraph cfg,
      Map<
              Entry<INode<Node<ControlFlowGraph.Edge, Val>>, Field>,
              INode<Node<ControlFlowGraph.Edge, Val>>>
          genField,
      BackwardQuery query,
      BoomerangOptions options,
      NestedWeightedPAutomatons<ControlFlowGraph.Edge, INode<Val>, W> callSummaries,
      NestedWeightedPAutomatons<Field, INode<Node<ControlFlowGraph.Edge, Val>>, W> fieldSummaries,
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

  public INode<Node<ControlFlowGraph.Edge, Val>> generateFieldState(
      final INode<Node<ControlFlowGraph.Edge, Val>> d, final Field loc) {
    Entry<INode<Node<Edge, Val>>, Field> e = new SimpleEntry<>(d, loc);
    if (!generatedFieldState.containsKey(e)) {
      generatedFieldState.put(
          e, new GeneratedState<>(new SingleNode<>(new Node<>(epsilonStmt(), Val.zero())), loc));
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

  protected void callFlow(Method caller, Node<Edge, Val> curr, Statement callSite) {
    icfg.addCalleeListener(new CallSiteCalleeListener(curr, caller));
    InvokeExpr invokeExpr = callSite.getInvokeExpr();
    if (dataFlowScope.isExcluded(invokeExpr.getMethod())) {
      byPassFlowAtCallsite(caller, curr);
    }
  }

  private void byPassFlowAtCallsite(Method caller, Node<Edge, Val> curr) {
    normalFlow(caller, curr);
    for (Statement returnSite :
        curr.stmt()
            .getStart()
            .getMethod()
            .getControlFlowGraph()
            .getPredsOf(curr.stmt().getStart())) {

      for (State s :
          getEmptyCalleeFlow(caller, new Edge(curr.stmt().getStart(), returnSite), curr.fact())) {
        propagate(curr, s);
      }
    }
  }

  @Override
  public void computeSuccessor(Node<Edge, Val> node) {
    LOGGER.trace("BW: Computing successor of {} for {}", node, this);
    Edge edge = node.stmt();
    Val value = node.fact();
    assert !(value instanceof AllocVal);
    Method method = edge.getStart().getMethod();
    if (method == null) return;
    if (dataFlowScope.isExcluded(method)) return;
    if (killFlow(method, edge.getStart(), value)) {
      return;
    }
    if (edge.getStart().containsInvokeExpr() && edge.getStart().uses(value) && INTERPROCEDURAL) {
      callFlow(method, node, edge.getStart());
    } else if (icfg.isExitStmt(edge.getStart())) {
      returnFlow(method, node);
    } else {
      normalFlow(method, node);
    }
  }

  protected void normalFlow(Method method, Node<ControlFlowGraph.Edge, Val> currNode) {
    Edge curr = currNode.stmt();
    Val value = currNode.fact();
    for (Statement pred :
        curr.getStart().getMethod().getControlFlowGraph().getPredsOf(curr.getStart())) {
      Collection<State> flow = computeNormalFlow(method, new Edge(pred, curr.getStart()), value);
      for (State s : flow) {
        propagate(currNode, s);
      }
    }
  }

  protected Collection<? extends State> computeCallFlow(
      Edge callSiteEdge, InvokeExpr invokeExpr, Val fact, Method callee, Edge calleeStartEdge) {
    Statement calleeSp = calleeStartEdge.getTarget();
    if (calleeSp.isThrowStmt()) {
      return Collections.emptySet();
    }
    Set<State> out = Sets.newHashSet();
    if (invokeExpr.isInstanceInvokeExpr()) {
      if (invokeExpr.getBase().equals(fact) && !callee.isStatic()) {
        out.add(
            new PushNode<>(calleeStartEdge, callee.getThisLocal(), callSiteEdge, PDSSystem.CALLS));
      }
    }
    List<Val> parameterLocals = callee.getParameterLocals();
    int i = 0;
    for (Val arg : invokeExpr.getArgs()) {
      if (arg.equals(fact) && parameterLocals.size() > i) {
        Val param = parameterLocals.get(i);
        out.add(new PushNode<>(calleeStartEdge, param, callSiteEdge, PDSSystem.CALLS));
      }
      i++;
    }

    Statement callSite = callSiteEdge.getTarget();
    if (callSite.isAssign() && calleeSp.isReturnStmt()) {
      if (callSite.getLeftOp().equals(fact)) {
        out.add(
            new PushNode<>(calleeStartEdge, calleeSp.getReturnOp(), callSiteEdge, PDSSystem.CALLS));
      }
    }
    if (fact.isStatic()) {
      out.add(
          new PushNode<>(
              calleeStartEdge, fact.withNewMethod(callee), callSiteEdge, PDSSystem.CALLS));
    }
    return out;
  }

  @Override
  public void processPush(
      Node<Edge, Val> curr, Location location, PushNode<Edge, Val, ?> succ, PDSSystem system) {
    if (PDSSystem.CALLS == system) {
      if (!((PushNode<Edge, Val, Edge>) succ).location().getTarget().equals(curr.stmt().getStart())
          || !(curr.stmt().getStart().containsInvokeExpr())) {
        throw new RuntimeException("Invalid push rule");
      }
    }
    super.processPush(curr, location, succ, system);
  }

  @Override
  protected Collection<State> computeNormalFlow(Method method, Edge currEdge, Val fact) {
    // BW data-flow inverses, therefore we
    Statement curr = currEdge.getTarget();
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
              out.add(new PushNode<>(currEdge, ifr.getX(), ifr.getY(), PDSSystem.FIELDS));
            }
          }
        } else if (curr.isStaticFieldLoad()) {
          if (options.trackFields()) {
            strategies
                .getStaticFieldStrategy()
                .handleBackward(currEdge, curr.getLeftOp(), curr.getStaticField(), out, this);
          }
        } else if (rightOp.isArrayRef()) {
          Pair<Val, Integer> arrayBase = curr.getArrayBase();
          if (options.trackFields()) {
            strategies.getArrayHandlingStrategy().handleBackward(currEdge, arrayBase, out, this);
          }
          // leftSideMatches = false;
        } else if (rightOp.isCast()) {
          out.add(new Node<>(currEdge, rightOp.getCastOp()));
        } else if (curr.isPhiStatement()) {
          Collection<Val> phiVals = curr.getPhiVals();
          for (Val v : phiVals) {
            out.add(new Node<>(currEdge, v));
          }
        } else {
          if (curr.isFieldLoadWithBase(fact)) {
            out.add(new ExclusionNode<>(currEdge, fact, curr.getLoadedField()));
          } else {
            out.add(new Node<>(currEdge, rightOp));
          }
        }
      }
      if (curr.isFieldStore()) {
        Pair<Val, Field> ifr = curr.getFieldStore();
        Val base = ifr.getX();
        if (base.equals(fact)) {
          NodeWithLocation<Edge, Val, Field> succNode =
              new NodeWithLocation<>(currEdge, rightOp, ifr.getY());
          out.add(new PopNode<>(succNode, PDSSystem.FIELDS));
        }
      } else if (curr.isStaticFieldStore()) {
        StaticFieldVal staticField = curr.getStaticField();
        if (fact.isStatic() && fact.equals(staticField)) {
          out.add(new Node<>(currEdge, rightOp));
        }
      } else if (leftOp.isArrayRef()) {
        Pair<Val, Integer> arrayBase = curr.getArrayBase();
        if (arrayBase.getX().equals(fact)) {
          NodeWithLocation<Edge, Val, Field> succNode =
              new NodeWithLocation<>(currEdge, rightOp, Field.array(arrayBase.getY()));
          out.add(new PopNode<>(succNode, PDSSystem.FIELDS));
        }
      }
    }
    if (!leftSideMatches) out.add(new Node<>(currEdge, fact));
    return out;
  }

  @Override
  public void applyCallSummary(
      Edge callSiteEdge, Val factAtSpInCallee, Edge spInCallee, Edge exitStmt, Val exitingFact) {
    Set<Node<Edge, Val>> out = Sets.newHashSet();
    Statement callSite = callSiteEdge.getTarget();
    if (callSite.containsInvokeExpr()) {
      if (exitingFact.isThisLocal()) {
        if (callSite.getInvokeExpr().isInstanceInvokeExpr()) {
          out.add(new Node<>(callSiteEdge, callSite.getInvokeExpr().getBase()));
        }
      }
      if (exitingFact.isReturnLocal()) {
        if (callSite.isAssign()) {
          out.add(new Node<>(callSiteEdge, callSite.getLeftOp()));
        }
      }
      for (int i = 0; i < callSite.getInvokeExpr().getArgs().size(); i++) {
        if (exitingFact.isParameterLocal(i)) {
          out.add(new Node<>(callSiteEdge, callSite.getInvokeExpr().getArg(i)));
        }
      }
    }
    for (Node<Edge, Val> xs : out) {
      addNormalCallFlow(new Node<>(callSiteEdge, exitingFact), xs);
      addNormalFieldFlow(new Node<>(exitStmt, exitingFact), xs);
    }
  }

  @Override
  protected void propagateUnbalancedToCallSite(
      Statement callSite, Transition<Edge, INode<Val>> transInCallee) {
    GeneratedState<Val, Edge> target = (GeneratedState<Val, Edge>) transInCallee.getTarget();

    if (!callSite.containsInvokeExpr()) {
      throw new RuntimeException("Invalid propagate Unbalanced return");
    }
    assertCalleeCallerRelation(callSite, transInCallee.getLabel().getMethod());
    cfg.addSuccsOfListener(
        new SuccessorListener(callSite) {
          @Override
          public void getSuccessor(Statement succ) {
            cfg.addPredsOfListener(
                new PredecessorListener(callSite) {
                  @Override
                  public void getPredecessor(Statement pred) {
                    Node<ControlFlowGraph.Edge, Val> curr =
                        new Node<>(new Edge(callSite, succ), query.var());

                    Transition<ControlFlowGraph.Edge, INode<Val>> callTrans =
                        new Transition<>(
                            wrap(curr.fact()),
                            curr.stmt(),
                            generateCallState(wrap(curr.fact()), curr.stmt()));
                    callAutomaton.addTransition(callTrans);
                    callAutomaton.addUnbalancedState(
                        generateCallState(wrap(curr.fact()), curr.stmt()), target);

                    State s =
                        new PushNode<>(
                            target.location(),
                            target.node().fact(),
                            new Edge(pred, callSite),
                            PDSSystem.CALLS);
                    propagate(curr, s);
                  }
                });
          }
        });
  }

  private final class CallSiteCalleeListener implements CalleeListener<Statement, Method> {
    private final Statement callSite;
    private final Node<Edge, Val> curr;
    private final Method caller;

    private CallSiteCalleeListener(Node<Edge, Val> curr, Method caller) {
      this.curr = curr;
      this.callSite = curr.stmt().getStart();
      this.caller = caller;
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
        for (Statement predOfCall :
            callSite.getMethod().getControlFlowGraph().getPredsOf(callSite)) {
          Collection<? extends State> res =
              computeCallFlow(
                  new Edge(predOfCall, callSite),
                  invokeExpr,
                  curr.fact(),
                  callee,
                  new Edge(calleeSp, calleeSp));
          for (State o : res) {
            BackwardBoomerangSolver.this.propagate(curr, o);
          }
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
