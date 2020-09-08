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
package typestate.finiteautomata;

import boomerang.WeightedForwardQuery;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SootClass;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import typestate.TransitionFunction;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;

public abstract class TypeStateMachineWeightFunctions
    implements WeightFunctions<Edge, Val, Edge, TransitionFunction> {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(TypeStateMachineWeightFunctions.class);
  public Set<MatcherTransition> transition = new HashSet<>();

  public void addTransition(MatcherTransition trans) {
    transition.add(trans);
  }

  @Override
  public TransitionFunction getOne() {
    return TransitionFunction.one();
  }

  public TransitionFunction pop(Node<Edge, Val> curr) {
    LOGGER.trace("Getting pop weights for {} which returns to {}", curr);
    return getOne();
  }

  public TransitionFunction push(Node<Edge, Val> curr, Node<Edge, Val> succ, Edge push) {
    return getMatchingTransitions(
        succ.stmt(),
        succ.fact(),
        push,
        Collections2.filter(
            transition,
            input ->
                input.getType().equals(Type.OnCall)
                    || input.getType().equals(Type.OnCallOrOnCallToReturn)),
        Type.OnCall);
  }

  @Override
  public TransitionFunction normal(Node<Edge, Val> curr, Node<Edge, Val> succ) {
    if (succ.stmt().getTarget().containsInvokeExpr()) {
      return callToReturn(curr, succ, succ.stmt().getTarget().getInvokeExpr());
    }
    return getOne();
  }

  public TransitionFunction callToReturn(
      Node<Edge, Val> curr, Node<Edge, Val> succ, InvokeExpr invokeExpr) {
    Set<Transition> res = Sets.newHashSet();
    if (invokeExpr.isInstanceInvokeExpr()) {
      if (invokeExpr.getBase().equals(succ.fact())) {
        for (MatcherTransition trans : transition) {
          if (trans.matches(invokeExpr.getMethod())
              && (trans.getType().equals(Type.OnCallToReturn)
                  || trans.getType().equals(Type.OnCallOrOnCallToReturn))) {
            res.add(trans);
          }
        }
      }
    }
    if (!res.isEmpty()) {
      LOGGER.trace("Typestate transition at {} to {}, [{}]", succ.stmt(), res, Type.OnCallToReturn);
    }
    return (res.isEmpty()
        ? getOne()
        : new TransitionFunction(res, Collections.singleton(succ.stmt())));
  }

  private TransitionFunction getMatchingTransitions(
      Edge edge,
      Val node,
      Edge transitionEdge,
      Collection<MatcherTransition> filteredTrans,
      Type type) {
    Statement transitionStmt = transitionEdge.getStart();
    Set<ITransition> res = new HashSet<>();
    if (filteredTrans.isEmpty() || !transitionStmt.containsInvokeExpr()) return getOne();
    for (MatcherTransition trans : filteredTrans) {
      if (trans.matches(transitionStmt.getInvokeExpr().getMethod())) {
        LOGGER.trace(
            "Found potential transition at {}, now checking if parameter match", transitionStmt);
        Parameter param = trans.getParam();
        if (param.equals(Parameter.This) && edge.getMethod().isThisLocal(node))
          res.add(new Transition(trans.from(), trans.to()));
        if (param.equals(Parameter.Param1) && edge.getMethod().getParameterLocal(0).equals(node))
          res.add(new Transition(trans.from(), trans.to()));
        if (param.equals(Parameter.Param2) && edge.getMethod().getParameterLocal(1).equals(node))
          res.add(new Transition(trans.from(), trans.to()));
      }
    }

    if (res.isEmpty()) return getOne();

    LOGGER.debug("Typestate transition at {} to {}, [{}]", transitionStmt, res, type);
    return new TransitionFunction(res, Collections.singleton(transitionEdge));
  }

  protected List<SootClass> getSubclassesOf(String className) {
    SootClass sootClass = Scene.v().getSootClass(className);
    List<SootClass> list = Scene.v().getActiveHierarchy().getSubclassesOfIncluding(sootClass);
    List<SootClass> res = new LinkedList<>();
    for (SootClass c : list) {
      res.add(c);
    }
    return res;
  }

  protected Collection<WeightedForwardQuery<TransitionFunction>> getLeftSideOf(Edge edge) {
    Statement s = edge.getStart();
    if (s.isAssign()) {
      return Collections.singleton(
          new WeightedForwardQuery<>(
              edge, new AllocVal(s.getLeftOp(), s, s.getRightOp()), initialTransition()));
    }
    return Collections.emptySet();
  }

  protected Collection<WeightedForwardQuery<TransitionFunction>> generateAtAllocationSiteOf(
      Edge edge, Class allocationSuperType) {
    Statement s = edge.getStart();
    if (s.isAssign()) {
      if (s.getRightOp().isNewExpr()) {
        boomerang.scene.Type newExprType = s.getRightOp().getNewExprType();
        if (newExprType.isSubtypeOf(allocationSuperType.getName())) {
          return Collections.singleton(
              new WeightedForwardQuery<>(
                  edge, new AllocVal(s.getLeftOp(), s, s.getRightOp()), initialTransition()));
        }
      }
    }
    return Collections.emptySet();
  }

  public Collection<WeightedForwardQuery<TransitionFunction>> generateThisAtAnyCallSitesOf(
      Edge edge, String declaredType, String declaredMethod) {
    Statement unit = edge.getStart();
    if (unit.containsInvokeExpr()) {
      if (unit.getInvokeExpr().isInstanceInvokeExpr()) {
        Val base = unit.getInvokeExpr().getBase();
        if (unit.getInvokeExpr().getMethod().getSignature().matches(declaredMethod)) {
          if (base.getType().isSubtypeOf(declaredType)) {
            return Collections.singleton(
                new WeightedForwardQuery<>(
                    edge, new AllocVal(base, unit, base), initialTransition()));
          }
        }
      }
    }
    return Collections.emptySet();
  }

  @Override
  public String toString() {
    return Joiner.on("\n").join(transition);
  }

  public abstract Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(Edge stmt);

  public TransitionFunction initialTransition() {
    return new TransitionFunction(
        new Transition(initialState(), initialState()), Collections.emptySet());
  }

  protected abstract State initialState();
}
