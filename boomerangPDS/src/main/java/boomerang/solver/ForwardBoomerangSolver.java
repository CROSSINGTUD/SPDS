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
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
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
import wpds.interfaces.Location;
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
      Map<Entry<INode<Node<Edge, Val>>, Field>, INode<Node<Edge, Val>>> genField,
      BoomerangOptions options,
      NestedWeightedPAutomatons<ControlFlowGraph.Edge, INode<Val>, W> callSummaries,
      NestedWeightedPAutomatons<Field, INode<Node<ControlFlowGraph.Edge, Val>>, W> fieldSummaries,
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

  @Override
  public void processPush(
      Node<Edge, Val> curr, Location location, PushNode<Edge, Val, ?> succ, PDSSystem system) {
    if (PDSSystem.CALLS == system) {
      if (!((PushNode<Edge, Val, Edge>) succ).location().getStart().equals(curr.stmt().getTarget())
          || !curr.stmt().getTarget().containsInvokeExpr()) {
        throw new RuntimeException("Invalid push rule");
      }
    }
    super.processPush(curr, location, succ, system);
  }

  private final class OverwriteAtFieldStore
      extends WPAStateListener<Field, INode<Node<ControlFlowGraph.Edge, Val>>, W> {
    private final Edge nextStmt;

    private OverwriteAtFieldStore(INode<Node<Edge, Val>> state, Edge nextEdge) {
      super(state);
      this.nextStmt = nextEdge;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<ControlFlowGraph.Edge, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Edge, Val>>, W> weightedPAutomaton) {
      if (t.getLabel().equals(nextStmt.getTarget().getFieldStore().getY())) {
        LOGGER.trace("Overwriting field {} at {}", t.getLabel(), nextStmt);
        overwriteFieldAtStatement(nextStmt, t);
      }
    }

    @Override
    public void onInTransitionAdded(
        Transition<Field, INode<Node<ControlFlowGraph.Edge, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Edge, Val>>, W> weightedPAutomaton) {}

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
      extends WPAStateListener<Field, INode<Node<Edge, Val>>, W> {
    private final Edge nextStmt;

    private OverwriteAtArrayStore(INode<Node<Edge, Val>> state, Edge nextStmt) {
      super(state);
      this.nextStmt = nextStmt;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Field, INode<Node<Edge, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Edge, Val>>, W> weightedPAutomaton) {
      if (t.getLabel().equals(Field.array(nextStmt.getTarget().getArrayBase().getY()))) {
        LOGGER.trace("Overwriting field {} at {}", t.getLabel(), nextStmt);
        overwriteFieldAtStatement(nextStmt, t);
      }
    }

    @Override
    public void onInTransitionAdded(
        Transition<Field, INode<Node<Edge, Val>>> t,
        W w,
        WeightedPAutomaton<Field, INode<Node<Edge, Val>>, W> weightedPAutomaton) {}

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
      Statement callSite, Transition<ControlFlowGraph.Edge, INode<Val>> transInCallee) {
    GeneratedState<Val, Edge> target = (GeneratedState<Val, Edge>) transInCallee.getTarget();
    if(!callSite.containsInvokeExpr()){
      throw new RuntimeException("Invalid propagate Unbalanced return");
    }
    assertCalleeCallerRelation(callSite, transInCallee.getLabel().getMethod());
    cfg.addSuccsOfListener(
        new SuccessorListener(callSite) {
          @Override
          public void getSuccessor(Statement succ) {
            cfg.addPredsOfListener(new PredecessorListener(callSite) {
              @Override
              public void getPredecessor(Statement pred) {
                Node<ControlFlowGraph.Edge, Val> curr = new Node<>(new Edge(pred,callSite), query.var());
                /**
                 * Transition<Field, INode<Node<Statement, Val>>> fieldTrans = new Transition<>(new
                 * SingleNode<>(curr), emptyField(), new SingleNode<>(curr));
                 * fieldAutomaton.addTransition(fieldTrans);*
                 */
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
                        new Edge(callSite, succ),
                        PDSSystem.CALLS);
                propagate(curr, s);
              }
            });
          }
        });
  }

  private final class CallSiteCalleeListener implements CalleeListener<Statement, Method> {
    private final Method caller;
    private final Statement callSite;
    private final Edge callSiteEdge;
    private final Node<ControlFlowGraph.Edge, Val> currNode;
    private final InvokeExpr invokeExpr;

    private CallSiteCalleeListener(
        Method caller,
        Edge callSiteEdge,
        Node<ControlFlowGraph.Edge, Val> currNode,
        InvokeExpr invokeExpr) {
      this.caller = caller;
      this.callSiteEdge = callSiteEdge;
      this.callSite = callSiteEdge.getStart();
      this.currNode = currNode;
      this.invokeExpr = invokeExpr;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((callSiteEdge == null) ? 0 : callSiteEdge.hashCode());
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
      if (callSiteEdge == null) {
        if (other.callSiteEdge != null) return false;
      } else if (!callSiteEdge.equals(other.callSiteEdge)) return false;
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
                callSite,
                callSiteEdge,
                invokeExpr,
                currNode,
                callee,
                new Edge(calleeSp, calleeSp));
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
      ControlFlowGraph.Edge returnSiteStatement,
      Val factInCallee,
      Edge spInCallee,
      Edge lastCfgEdgeInCallee,
      Val returnedFact) {
    Statement callSite = returnSiteStatement.getStart();

    Set<Node<ControlFlowGraph.Edge, Val>> out = Sets.newHashSet();
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
              returnSiteStatement,
              returnedFact.withNewMethod(returnSiteStatement.getStart().getMethod())));
    }
    for (Node<ControlFlowGraph.Edge, Val> xs : out) {
      addNormalCallFlow(new Node<>(returnSiteStatement, returnedFact), xs);
      addNormalFieldFlow(new Node<>(lastCfgEdgeInCallee, returnedFact), xs);
    }
  }

  public Collection<? extends State> computeCallFlow(
      Method caller,
      Statement callSite,
      Edge succOfCallSite,
      InvokeExpr invokeExpr,
      Node<ControlFlowGraph.Edge, Val> currNode,
      Method callee,
      Edge calleeStartEdge) {
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
            new PushNode<>(
                calleeStartEdge, callee.getThisLocal(), succOfCallSite, PDSSystem.CALLS));
      }
    }
    int i = 0;
    List<Val> parameterLocals = callee.getParameterLocals();
    for (Val arg : invokeExpr.getArgs()) {
      if (arg.equals(fact) && parameterLocals.size() > i) {
        Val param = parameterLocals.get(i);
        out.add(new PushNode<>(calleeStartEdge, param, succOfCallSite, PDSSystem.CALLS));
      }
      i++;
    }
    if (fact.isStatic()) {
      out.add(
          new PushNode<>(
              calleeStartEdge, fact.withNewMethod(callee), succOfCallSite, PDSSystem.CALLS));
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
  public void computeSuccessor(Node<Edge, Val> node) {
    Edge curr = node.stmt();
    Val value = node.fact();
    assert !(value instanceof AllocVal);
    Method method = curr.getTarget().getMethod();
    if (method == null) return;
    if (dataFlowScope.isExcluded(method)) {
      return;
    }
    if (icfg.isExitStmt(curr.getTarget())) {
      returnFlow(method, node);
      return;
    }
    cfg.addSuccsOfListener(
        new SuccessorListener(curr.getTarget()) {

          @Override
          public void getSuccessor(Statement succ) {
            if (query.getType().isNullType()
                && curr.getStart().isIfStmt()
                && curr.getStart().killAtIfStmt(value, succ)) {
              return;
            }

            if (curr.getTarget().containsInvokeExpr()
                && (curr.getTarget().isParameter(value) || value.isStatic())) {
              callFlow(
                  method, node, new Edge(curr.getTarget(), succ), curr.getTarget().getInvokeExpr());
            } else if (!killFlow(method, succ, value)) {
              checkForFieldOverwrite(curr, value);
              Collection<State> out =
                  computeNormalFlow(method, new Edge(curr.getTarget(), succ), value);
              for (State s : out) {
                LOGGER.trace("{}: {} -> {}", s, node, ForwardBoomerangSolver.this.query);
                propagate(node, s);
              }
            }
          }
        });
  }

  private void checkForFieldOverwrite(Edge curr, Val value) {
    if ((curr.getTarget().isFieldStore()
        && curr.getTarget().getFieldStore().getX().equals(value))) {
      Node<ControlFlowGraph.Edge, Val> node = new Node<>(curr, value);
      fieldAutomaton.registerListener(new OverwriteAtFieldStore(new SingleNode<>(node), curr));
    } else if ((curr.getTarget().isArrayStore()
        && curr.getTarget().getArrayBase().getX().equals(value))) {
      Node<ControlFlowGraph.Edge, Val> node = new Node<>(curr, value);
      fieldAutomaton.registerListener(new OverwriteAtArrayStore(new SingleNode<>(node), curr));
    }
  }

  protected abstract void overwriteFieldAtStatement(
      Edge fieldWriteStatementEdge, Transition<Field, INode<Node<Edge, Val>>> killedTransition);

  @Override
  public Collection<State> computeNormalFlow(Method method, Edge nextEdge, Val fact) {
    Statement succ = nextEdge.getStart();
    Set<State> out = Sets.newHashSet();
    if (!succ.isFieldWriteWithBase(fact)) {
      // always maintain data-flow if not a field write // killFlow has
      // been taken care of
      if (!options.trackReturnOfInstanceOf()
          || !(query.getType().isNullType() && succ.isInstanceOfStatement(fact))) {
        out.add(new Node<>(nextEdge, fact));
      }
    } else {
      out.add(new ExclusionNode<>(nextEdge, fact, succ.getWrittenField()));
    }
    if (succ.isAssign()) {
      Val leftOp = succ.getLeftOp();
      Val rightOp = succ.getRightOp();
      if (rightOp.equals(fact)) {
        if (succ.isFieldStore()) {
          Pair<Val, Field> ifr = succ.getFieldStore();
          if (options.trackFields()) {
            if (!options.ignoreInnerClassFields() || !ifr.getY().isInnerClassField()) {
              out.add(new PushNode<>(nextEdge, ifr.getX(), ifr.getY(), PDSSystem.FIELDS));
            }
          }
        } else if (succ.isStaticFieldStore()) {
          StaticFieldVal sf = succ.getStaticField();
          if (options.trackFields()) {
            strategies.getStaticFieldStrategy().handleForward(nextEdge, rightOp, sf, out, this);
          }
        } else if (leftOp.isArrayRef()) {
          Pair<Val, Integer> arrayBase = succ.getArrayBase();
          if (options.trackFields()) {
            strategies.getArrayHandlingStrategy().handleForward(nextEdge, arrayBase, out, this);
          }
        } else {
          out.add(new Node<>(nextEdge, leftOp));
        }
      }
      if (succ.isFieldLoad()) {
        Pair<Val, Field> ifr = succ.getFieldLoad();
        if (ifr.getX().equals(fact)) {
          NodeWithLocation<Edge, Val, Field> succNode =
              new NodeWithLocation<>(nextEdge, leftOp, ifr.getY());
          out.add(new PopNode<>(succNode, PDSSystem.FIELDS));
        }
      } else if (succ.isStaticFieldLoad()) {
        StaticFieldVal sf = succ.getStaticField();
        if (fact.isStatic() && fact.equals(sf)) {
          out.add(new Node<>(nextEdge, leftOp));
        }
      } else if (rightOp.isArrayRef()) {
        Pair<Val, Integer> arrayBase = succ.getArrayBase();
        if (arrayBase.getX().equals(fact)) {
          NodeWithLocation<Edge, Val, Field> succNode =
              new NodeWithLocation<>(nextEdge, leftOp, Field.array(arrayBase.getY()));

          //                    out.add(new Node<Statement, Val>(succ, leftOp));
          out.add(new PopNode<>(succNode, PDSSystem.FIELDS));
        }
      } else if (rightOp.isCast()) {
        if (rightOp.getCastOp().equals(fact)) {
          out.add(new Node<>(nextEdge, leftOp));
        }
      } else if (rightOp.isInstanceOfExpr()
          && query.getType().isNullType()
          && options.trackReturnOfInstanceOf()) {
        if (rightOp.getInstanceOfOp().equals(fact)) {
          out.add(new Node<>(nextEdge, fact.withSecondVal(leftOp)));
        }
      } else if (succ.isPhiStatement()) {
        Collection<Val> phiVals = succ.getPhiVals();
        if (phiVals.contains(fact)) {
          out.add(new Node<>(nextEdge, succ.getLeftOp()));
        }
      }
    }

    return out;
  }

  protected void callFlow(
      Method caller,
      Node<ControlFlowGraph.Edge, Val> currNode,
      Edge callSiteEdge,
      InvokeExpr invokeExpr) {
    assert icfg.isCallStmt(callSiteEdge.getStart());
    if (dataFlowScope.isExcluded(invokeExpr.getMethod())) {
      byPassFlowAtCallSite(caller, currNode, callSiteEdge.getStart());
    }

    icfg.addCalleeListener(new CallSiteCalleeListener(caller, callSiteEdge, currNode, invokeExpr));
  }

  private void byPassFlowAtCallSite(
      Method caller, Node<ControlFlowGraph.Edge, Val> currNode, Statement callSite) {
    LOGGER.trace(
        "Bypassing call flow of {} at callsite: {} for {}", currNode.fact(), callSite, this);

    cfg.addSuccsOfListener(
        new SuccessorListener(currNode.stmt().getTarget()) {

          @Override
          public void getSuccessor(Statement returnSite) {
            for (State s :
                getEmptyCalleeFlow(caller, new Edge(callSite, returnSite), currNode.fact())) {
              propagate(currNode, s);
            }
            for (State s :
                computeNormalFlow(caller, new Edge(callSite, returnSite), currNode.fact())) {
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

  @Override
  public String toString() {
    return "ForwardSolver: " + query;
  }
}
