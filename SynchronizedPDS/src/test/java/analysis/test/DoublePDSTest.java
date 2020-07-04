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
package analysis.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.SyncPDSSolver;
import sync.pds.solver.SyncPDSSolver.PDSSystem;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.ExclusionNode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.SummaryNestedWeightedPAutomatons;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.wildcard.ExclusionWildcard;
import wpds.wildcard.Wildcard;

public class DoublePDSTest {
  private static Logger LOGGER = LoggerFactory.getLogger(DoublePDSTest.class);
  private Multimap<Node<Statement, Variable>, State> successorMap = HashMultimap.create();
  private Multimap<Node<Statement, Variable>, Node<Statement, Variable>> summaryMap =
      HashMultimap.create();

  private void addFieldPop(
      Node<Statement, Variable> curr, FieldRef ref, Node<Statement, Variable> succ) {
    addSucc(
        curr,
        new PopNode<>(new NodeWithLocation<>(succ.stmt(), succ.fact(), ref), PDSSystem.FIELDS));
  }

  private void addFieldPush(
      Node<Statement, Variable> curr, FieldRef push, Node<Statement, Variable> succ) {
    addSucc(curr, new PushNode<>(succ.stmt(), succ.fact(), push, PDSSystem.FIELDS));
  }

  private void addNormal(Node<Statement, Variable> curr, Node<Statement, Variable> succ) {
    addSucc(curr, succ);
  }

  private void addReturnFlow(Node<Statement, Variable> curr, Variable returns) {
    addSucc(curr, new PopNode<>(returns, PDSSystem.CALLS));
  }

  private void addCallFlow(
      Node<Statement, Variable> curr, Node<Statement, Variable> succ, Statement returnSite) {
    addSucc(curr, new PushNode<>(succ.stmt(), succ.fact(), returnSite, PDSSystem.CALLS));
  }

  private void calleeToCallerMapping(
      Node<Statement, Variable> ret, Node<Statement, Variable> succOfRet) {
    summaryMap.put(ret, succOfRet);
  }

  private void addSucc(Node<Statement, Variable> curr, State succ) {
    successorMap.put(curr, succ);
  }

  private void addExcludeField(
      Node<Statement, Variable> curr, FieldRef push, Node<Statement, Variable> succ) {
    addSucc(curr, new ExclusionNode<>(succ.stmt(), succ.fact(), push));
  }

  private FieldRef epsilonField = new FieldRef("EMPTY");
  private Statement epsilonCallSite = new Statement(-1);

  private SyncPDSSolver<Statement, Variable, FieldRef, NoWeight> solver = new TestSyncPDSSolver();

  private class TestSyncPDSSolver extends SyncPDSSolver<Statement, Variable, FieldRef, NoWeight> {

    public TestSyncPDSSolver() {
      super(
          false,
          new SummaryNestedWeightedPAutomatons<>(),
          false,
          new SummaryNestedWeightedPAutomatons<>(),
          -1,
          -1,
          -1);
    }

    @Override
    public void computeSuccessor(Node<Statement, Variable> node) {
      Collection<State> states = successorMap.get(node);
      for (State s : states) {
        propagate(node, s);
      }
    }

    @Override
    public FieldRef epsilonField() {
      return new FieldRef("eps_f");
    }

    @Override
    public Statement epsilonStmt() {
      return epsilonCallSite;
    }

    @Override
    public FieldRef emptyField() {
      return epsilonField;
    }

    @Override
    public FieldRef fieldWildCard() {
      return new FieldWildCard();
    }

    @Override
    public FieldRef exclusionFieldWildCard(FieldRef exclusion) {
      return new ExclusionWildcardField(exclusion);
    }

    @Override
    public void applyCallSummary(
        Statement callSite,
        Variable factInCallee,
        Statement sPInCallee,
        Statement exitStmt,
        Variable exitingFact) {
      Node<Statement, Variable> exitingNode = new Node<>(exitStmt, exitingFact);
      for (Node<Statement, Variable> n : summaryMap.get(exitingNode)) {
        addNormalFieldFlow(exitingNode, n);
        addNormalCallFlow(new Node<>(callSite, exitingFact), n);
      }
    }

    @Override
    public WeightFunctions<Statement, Variable, FieldRef, NoWeight> getFieldWeights() {
      return new OneWeightFunctions<>(NoWeight.NO_WEIGHT_ONE);
    }

    @Override
    public WeightFunctions<Statement, Variable, Statement, NoWeight> getCallWeights() {
      return new OneWeightFunctions<>(NoWeight.NO_WEIGHT_ONE);
    }
  }

  private void solve(Node<Statement, Variable> node) {
    solver.solve(
        node,
        epsilonField,
        new SingleNode<>(node(1, "u")),
        epsilonCallSite,
        new SingleNode<>(new Variable("u")));

    LOGGER.info("All reachable states of SPDS: {}", solver.getReachedStates());
  }

  @Test
  public void test1() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addCallFlow(node(2, "v"), node(3, "p"), returnSite(5));
    addFieldPush(node(3, "p"), f("g"), node(4, "q"));
    addReturnFlow(node(4, "q"), var("q"));
    addFieldPop(node(5, "w"), f("g"), node(6, "x"));
    addFieldPop(node(6, "x"), f("f"), node(7, "y"));

    // second branch
    addFieldPush(node(8, "r"), f("f"), node(9, "s"));
    addCallFlow(node(9, "s"), node(3, "p"), returnSite(10));
    addReturnFlow(node(4, "q"), var("q"));
    addFieldPush(node(10, "t"), f("f"), node(11, "s"));

    calleeToCallerMapping(node(4, "q"), node(5, "w"));
    calleeToCallerMapping(node(4, "q"), node(10, "t"));

    solve(node(1, "u"));
    // TODO needs inspection
    // assertFalse(solver.getReachedStates().contains(node(11, "s")));
    assertTrue(solver.getReachedStates().contains(node(5, "w")));
    assertTrue(solver.getReachedStates().contains(node(6, "x")));
    assertFalse(solver.getReachedStates().contains(node(7, "y")));
  }

  @Test
  public void branching() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addFieldPush(node(1, "u"), f("g"), node(3, "x"));

    // can pop
    addFieldPop(node(2, "v"), f("h"), node(5, "y"));
    addFieldPop(node(3, "x"), f("g"), node(4, "y"));

    // but cannot pop
    addFieldPop(node(5, "y"), f("g"), node(6, "y"));
    addFieldPop(node(4, "y"), f("g"), node(7, "y"));
    solve(node(1, "u"));
    solver.debugOutput();
    assertTrue(solver.getReachedStates().contains(node(5, "y")));
    assertTrue(solver.getReachedStates().contains(node(4, "y")));
    assertFalse(solver.getReachedStates().contains(node(6, "y")));
    assertFalse(solver.getReachedStates().contains(node(7, "y")));
  }

  @Test
  public void tooMuchPopping() {
    addFieldPush(node(1, "u"), f("g"), node(3, "x"));
    addFieldPop(node(3, "x"), f("g"), node(3, "y"));
    addFieldPop(node(3, "y"), f("g"), node(3, "z"));
    solve(node(1, "u"));
    assertTrue(solver.getReachedStates().contains(node(3, "y")));
    assertFalse(solver.getReachedStates().contains(node(3, "z")));
  }

  @Test
  public void test1Simple() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addCallFlow(node(2, "v"), node(3, "p"), returnSite(5));
    addFieldPush(node(3, "p"), f("g"), node(4, "q"));
    addReturnFlow(node(4, "q"), var("q"));
    addFieldPop(node(5, "w"), f("g"), node(6, "x"));
    addFieldPop(node(6, "x"), f("f"), node(7, "y"));

    calleeToCallerMapping(node(4, "q"), node(5, "w"));
    solve(node(1, "u"));
    solver.debugOutput();
    assertTrue(solver.getReachedStates().contains(node(6, "x")));
  }

  @Test
  public void callOnlyIntraprocedural() {
    addNormal(node(1, "u"), node(5, "q"));
    addNormal(node(5, "q"), node(6, "x"));
    solve(node(1, "u"));
    assertTrue(solver.getReachedStates().contains(node(6, "x")));
  }

  @Test
  public void fieldPushAndPop() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addFieldPop(node(2, "v"), f("h"), node(6, "x"));
    solve(node(1, "u"));
    assertTrue(solver.getReachedStates().contains(node(6, "x")));
  }

  @Test
  public void simpleNonFieldFlow() {
    addNormal(node(1, "v"), node(2, "w"));
    addCallFlow(node(2, "w"), node(3, "p"), returnSite(4));
    addNormal(node(3, "p"), node(5, "q"));
    addNormal(node(5, "q"), node(7, "z"));
    addNormal(node(7, "z"), node(6, "x"));
    addReturnFlow(node(6, "x"), var("x"));
    addNormal(node(4, "k"), node(6, "y"));

    calleeToCallerMapping(node(6, "x"), node(4, "k"));
    solve(node(1, "v"));
    solver.debugOutput();
    assertTrue(solver.getReachedStates().contains(node(6, "y")));
  }

  @Test
  public void simpleExclusionFieldFlow() {
    addFieldPush(node(1, "v"), f("g"), node(4, "w"));
    addExcludeField(node(4, "w"), f("g"), node(5, "w"));
    addFieldPop(node(5, "w"), f("g"), node(7, "w"));

    solve(node(1, "v"));
    solver.debugOutput();
    assertFalse(solver.getReachedStates().contains(node(7, "w")));
  }

  @Test
  public void simpleNegativeExclusionFieldFlow() {
    addFieldPush(node(1, "v"), f("g"), node(4, "w"));
    addExcludeField(
        node(4, "w"), f("h"), node(5, "w")); // overwrite of h should not affect the subsequent pop
    // operation
    addFieldPop(node(5, "w"), f("g"), node(7, "w"));

    solve(node(1, "v"));
    solver.debugOutput();
    assertTrue(solver.getReachedStates().contains(node(7, "w")));
  }

  @Test
  public void doubleNegativeExclusionFieldFlow() {
    addFieldPush(node(1, "v"), f("g"), node(4, "w"));
    addExcludeField(
        node(4, "w"), f("h"), node(5, "w")); // overwrite of h should not affect the subsequent pop
    // operation
    addExcludeField(
        node(5, "w"), f("i"), node(6, "w")); // overwrite of h should not affect the subsequent pop
    // operation
    addFieldPop(node(6, "w"), f("g"), node(7, "w"));

    solve(node(1, "v"));
    solver.debugOutput();
    assertTrue(solver.getReachedStates().contains(node(7, "w")));
  }

  @Test
  public void doubleExclusionFieldFlow() {
    addFieldPush(node(1, "v"), f("g"), node(4, "w"));
    addExcludeField(
        node(4, "w"), f("i"), node(5, "w")); // overwrite of i should not affect the subsequent pop
    // operation
    addExcludeField(node(5, "w"), f("g"), node(6, "w"));
    addFieldPop(node(6, "w"), f("g"), node(7, "w"));

    solve(node(1, "v"));
    solver.debugOutput();
    assertFalse(solver.getReachedStates().contains(node(7, "w")));
  }

  @Test
  public void simpleTransitiveExclusionFieldFlow() {
    addFieldPush(node(1, "v"), f("g"), node(4, "w"));
    addExcludeField(node(4, "w"), f("g"), node(5, "w"));
    addNormal(node(5, "w"), node(6, "w"));
    addFieldPop(node(6, "w"), f("g"), node(7, "w"));

    solve(node(1, "v"));
    solver.debugOutput();
    assertFalse(solver.getReachedStates().contains(node(7, "w")));
  }

  @Test
  public void simpleNegativeTransitiveExclusionFieldFlow() {
    addFieldPush(node(1, "v"), f("g"), node(4, "w"));
    addExcludeField(
        node(4, "w"), f("h"), node(5, "w")); // overwrite of h should not affect the subsequent pop
    // operation
    addNormal(node(5, "w"), node(6, "w"));
    addFieldPop(node(6, "w"), f("g"), node(7, "w"));

    solve(node(1, "v"));
    solver.debugOutput();
    assertTrue(solver.getReachedStates().contains(node(7, "w")));
  }

  @Test
  public void testWithTwoStacks() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addCallFlow(node(2, "v"), node(3, "p"), returnSite(4));
    addFieldPush(node(3, "p"), f("g"), node(5, "q"));
    addReturnFlow(node(5, "q"), var("q"));
    addNormal(node(4, "w"), node(7, "t"));

    calleeToCallerMapping(node(5, "q"), node(4, "w"));
    solve(node(1, "u"));
    assertTrue(solver.getReachedStates().contains(node(7, "t")));
  }

  @Test
  public void testWithTwoStacksAndTwoField() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addCallFlow(node(2, "v"), node(3, "p"), returnSite(4));
    addFieldPush(node(3, "p"), f("g"), node(5, "q"));
    addFieldPush(node(5, "q"), f("f"), node(6, "q"));
    addReturnFlow(node(6, "q"), var("q"));
    addNormal(node(4, "w"), node(7, "t"));
    addFieldPop(node(7, "t"), f("f"), node(8, "s"));
    addFieldPop(node(8, "s"), f("g"), node(9, "x"));
    addFieldPop(node(9, "x"), f("h"), node(10, "y"));
    addFieldPop(node(9, "x"), f("impossibleRead"), node(11, "z"));

    calleeToCallerMapping(node(6, "q"), node(4, "w"));

    solve(node(1, "u"));
    solver.debugOutput();
    assertTrue(solver.getReachedStates().contains(node(7, "t")));
    assertTrue(solver.getReachedStates().contains(node(8, "s")));
    assertTrue(solver.getReachedStates().contains(node(9, "x")));
    assertTrue(solver.getReachedStates().contains(node(10, "y")));
    assertFalse(solver.getReachedStates().contains(node(11, "z")));
  }

  @Test
  public void positiveTestFieldDoublePushAndPop() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addFieldPush(node(2, "v"), f("g"), node(3, "w"));
    addFieldPop(node(3, "w"), f("g"), node(4, "x"));
    addNormal(node(3, "w"), node(4, "kkk"));
    addFieldPop(node(4, "x"), f("h"), node(5, "y"));
    solve(node(1, "u"));
    assertTrue(solver.getReachedStates().contains(node(4, "x")));
    assertTrue(solver.getReachedStates().contains(node(5, "y")));
  }

  @Test
  public void negativeTestFieldDoublePushAndPop() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addFieldPush(node(2, "v"), f("h"), node(3, "w"));
    addFieldPop(node(3, "w"), f("h"), node(4, "x"));
    addFieldPop(node(4, "x"), f("g"), node(5, "y"));
    solve(node(1, "u"));
    assertTrue(solver.getReachedStates().contains(node(4, "x")));
    assertFalse(solver.getReachedStates().contains(node(5, "y")));
  }

  @Test
  public void positiveTestFieldPushPushAndPop() {
    addFieldPush(node(0, "u"), f("h"), node(1, "u"));
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addFieldPop(node(2, "v"), f("h"), node(2, "x"));
    solve(node(0, "u"));
    solver.debugOutput();
    assertTrue(solver.getReachedStates().contains(node(2, "x")));
  }

  @Test
  public void negativeTestFieldPushAndPopPop() {
    addFieldPush(node(0, "u"), f("h"), node(1, "u"));
    addFieldPop(node(1, "u"), f("h"), node(2, "v"));
    addFieldPop(node(2, "v"), f("h"), node(2, "x"));
    solve(node(0, "u"));
    assertFalse(solver.getReachedStates().contains(node(2, "x")));
  }

  @Test
  public void negativeSinglePop() {
    addNormal(node(0, "u"), node(1, "u"));
    addFieldPop(node(1, "u"), f("h"), node(2, "v"));
    solve(node(0, "u"));
    assertFalse(solver.getReachedStates().contains(node(2, "v")));
  }

  @Test
  public void negativeJustPop() {
    addFieldPop(node(0, "u"), f("h"), node(2, "v"));
    solve(node(0, "u"));
    System.out.println(solver.getReachedStates());
    assertFalse(solver.getReachedStates().contains(node(2, "v")));
  }

  @Test
  public void positiveTestFieldPushAndPop() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addFieldPop(node(2, "v"), f("h"), node(2, "x"));
    solve(node(1, "u"));
    assertTrue(solver.getReachedStates().contains(node(2, "x")));
  }

  @Test
  public void positiveTestFieldIntermediatePushAndPop() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addNormal(node(2, "v"), node(3, "w"));
    addNormal(node(3, "w"), node(4, "w"));
    addFieldPop(node(4, "w"), f("h"), node(5, "w"));
    solve(node(1, "u"));
    assertTrue(solver.getReachedStates().contains(node(5, "w")));
  }

  @Test
  public void positiveTestFieldLoop() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addFieldPush(node(2, "v"), f("h"), node(2, "v"));
    addFieldPop(node(2, "v"), f("h"), node(3, "w"));
    addFieldPop(node(3, "w"), f("h"), node(4, "x"));
    addFieldPop(node(4, "x"), f("h"), node(5, "y"));
    solve(node(1, "u"));
    assertTrue(solver.getReachedStates().contains(node(4, "x")));
    assertTrue(solver.getReachedStates().contains(node(5, "y")));
  }

  @Test
  public void positiveTestFieldLoop2() {
    addFieldPush(node(0, "a"), f("g"), node(1, "u"));
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addFieldPush(node(2, "v"), f("h"), node(2, "v"));
    addFieldPop(node(2, "v"), f("h"), node(3, "w"));
    addFieldPop(node(3, "w"), f("h"), node(4, "x"));
    addFieldPop(node(4, "x"), f("g"), node(5, "y"));
    solve(node(0, "a"));
    assertTrue(solver.getReachedStates().contains(node(5, "y")));
  }

  @Test
  public void positiveSummaryTest() {
    // 1 :a.g = c
    // 4: foo(a)
    // 5: e = a.f
    // 6: foo(e)
    // 7: h = e.f

    // 2: foo(u)
    // 3: u.f = ...
    addFieldPush(node(0, "c"), f("g"), node(1, "a"));
    addCallFlow(node(1, "a"), node(2, "u"), returnSite(4));
    addFieldPush(node(2, "u"), f("f"), node(3, "u"));
    addReturnFlow(node(3, "u"), var("u"));

    addNormal(node(4, "a"), node(5, "e"));
    addCallFlow(node(5, "e"), node(2, "u"), returnSite(6));
    addReturnFlow(node(3, "u"), var("u"));
    addFieldPop(node(6, "a"), f("f"), node(7, "h"));
    addFieldPop(node(7, "h"), f("f"), node(8, "g"));
    addFieldPop(node(8, "g"), f("g"), node(9, "z"));
    addFieldPop(node(8, "g"), f("f"), node(9, "y"));

    calleeToCallerMapping(node(3, "u"), node(4, "a"));
    calleeToCallerMapping(node(3, "u"), node(6, "a"));
    solve(node(0, "c"));
    System.out.println(solver.getReachedStates());
    solver.debugOutput();
    assertTrue(solver.getReachedStates().contains(node(7, "h")));
    assertTrue(solver.getReachedStates().contains(node(8, "g")));
    assertTrue(solver.getReachedStates().contains(node(9, "z")));
    // assertFalse(solver.getReachedStates().contains( node(9,"y")));//False Positive
  }

  @Test
  public void positiveSummaryWithFieldTest() {
    addFieldPush(node(0, "c"), f("g"), node(1, "a"));
    addCallFlow(node(1, "a"), node(2, "u"), returnSite(4));
    addFieldPush(node(2, "u"), f("f"), node(3, "u"));
    addReturnFlow(node(3, "u"), var("u"));
    addFieldPop(node(4, "a"), f("f"), node(5, "e"));
    addCallFlow(node(5, "e"), node(2, "u"), returnSite(6));

    // addReturnFlow(node(3,"u"),var("e"));
    addFieldPop(
        node(6, "a"),
        f("f"),
        node(7, "h")); // Due to the summary, we should be able to read f again.
    addFieldPop(
        node(7, "h"),
        f("g"),
        node(8, "l")); // Due to the summary, we should be able to read f again.
    // addNormal(node(6,"e"), node(7,"h"));

    calleeToCallerMapping(node(3, "u"), node(4, "a"));
    calleeToCallerMapping(node(3, "u"), node(6, "a"));

    solve(node(0, "c"));
    System.out.println(solver.getReachedStates());
    solver.debugOutput();
    assertTrue(solver.getReachedStates().contains(node(7, "h")));
    assertTrue(solver.getReachedStates().contains(node(8, "l")));
  }

  @Test
  public void simpleFieldPushAndPopAndContext() {
    addFieldPush(node(0, "c"), f("g"), node(1, "a"));
    addCallFlow(node(1, "a"), node(2, "u"), returnSite(4));
    addFieldPush(node(2, "u"), f("f"), node(3, "u"));
    addReturnFlow(node(3, "u"), var("u"));
    addFieldPop(node(4, "a"), f("f"), node(5, "e"));
    addFieldPop(node(5, "e"), f("g"), node(6, "f")); // Should be possible

    calleeToCallerMapping(node(3, "u"), node(4, "a"));
    solve(node(0, "c"));
    assertTrue(solver.getReachedStates().contains(node(6, "f")));
  }

  @Test
  public void positiveNoFieldsSummaryTest() {
    addNormal(node(0, "c"), node(1, "a"));
    addCallFlow(node(1, "a"), node(2, "u"), returnSite(4));
    addNormal(node(2, "u"), node(3, "u"));
    addReturnFlow(node(3, "u"), var("u"));

    addNormal(node(4, "a"), node(5, "e"));
    addCallFlow(node(5, "e"), node(2, "u"), returnSite(6));
    addNormal(node(6, "e"), node(7, "h"));

    calleeToCallerMapping(node(3, "u"), node(4, "a"));
    calleeToCallerMapping(node(3, "u"), node(6, "e"));
    solve(node(0, "c"));
    assertTrue(solver.getReachedStates().contains(node(7, "h")));
  }

  @Test
  public void positiveSummaryFlowTest() {
    addCallFlow(node(1, "a"), node(2, "u"), returnSite(4));
    addReturnFlow(node(2, "u"), var("e"));
    addCallFlow(node(4, "e"), node(2, "u"), returnSite(6));

    calleeToCallerMapping(node(2, "u"), node(4, "e"));
    calleeToCallerMapping(node(2, "u"), node(6, "e"));

    solve(node(1, "a"));
    assertTrue(solver.getReachedStates().contains(node(6, "e")));
  }

  @Test
  public void recursion() {
    addCallFlow(node(1, "a"), node(2, "u"), returnSite(4));
    addNormal(node(2, "u"), node(3, "c"));
    addFieldPush(node(3, "c"), f("h"), node(4, "h"));
    addCallFlow(node(4, "h"), node(2, "u"), returnSite(5));
    addNormal(node(4, "h"), node(5, "h"));
    addFieldPop(node(5, "h"), f("h"), node(6, "g"));
    addFieldPop(node(6, "g"), f("h"), node(7, "g"));
    addReturnFlow(node(7, "g"), var("g"));

    calleeToCallerMapping(node(7, "g"), node(4, "a"));
    solve(node(1, "a"));
    assertTrue(solver.getReachedStates().contains(node(4, "a")));
  }

  @Test
  public void recursion2() {
    addCallFlow(node(1, "a"), node(2, "u"), returnSite(4));
    addNormal(node(2, "u"), node(3, "c"));
    addFieldPush(node(3, "c"), f("h"), node(4, "h"));
    addCallFlow(node(4, "h"), node(2, "u"), returnSite(5));
    addNormal(node(4, "h"), node(5, "h"));
    addFieldPop(node(5, "h"), f("h"), node(6, "g"));
    addFieldPop(node(6, "g"), f("h"), node(7, "g"));
    addReturnFlow(node(7, "g"), var("g"));

    calleeToCallerMapping(node(7, "g"), node(4, "a"));
    solve(node(1, "a"));
    assertTrue(solver.getReachedStates().contains(node(4, "a")));
  }

  @Test
  public void negativeTestFieldPushAndPop() {
    addFieldPush(node(1, "u"), f("h"), node(2, "v"));
    addFieldPop(node(2, "v"), f("f"), node(3, "w"));
    solve(node(1, "u"));
    assertFalse(solver.getReachedStates().contains(node(3, "w")));
  }

  @Test
  public void negativeTestCallSitePushAndPop() {
    addCallFlow(node(1, "u"), node(2, "v"), returnSite(4));
    addReturnFlow(node(2, "v"), var("w"));
    addNormal(node(3, "w"), node(4, "w"));
    solve(node(1, "u"));
    System.out.println(solver.getReachedStates());
    assertFalse(solver.getReachedStates().contains(node(3, "w")));
  }

  @Test
  public void positiveTestCallSitePushAndPop() {
    addCallFlow(node(1, "u"), node(4, "v"), returnSite(2));
    addReturnFlow(node(4, "v"), var("v"));
    addNormal(node(2, "w"), node(3, "w"));

    calleeToCallerMapping(node(4, "v"), node(2, "w"));
    solve(node(1, "u"));
    //   verify(solver,times(1)).applyCallSummary(any(),any(),any(),any(),any());
    assertTrue(solver.getReachedStates().contains(node(3, "w")));
  }

  private Variable var(String v) {
    return new Variable(v);
  }

  private static Statement returnSite(int call) {
    return new Statement(call);
  }

  private static FieldRef f(String f) {
    return new FieldRef(f);
  }

  public static Node<Statement, Variable> node(int stmt, String var) {
    return new Node<Statement, Variable>(new Statement(stmt), new Variable(var));
  }

  private static class Statement extends StringBasedObj implements Location {
    public Statement(int name) {
      super(Integer.toString(name));
    }

    @Override
    public boolean accepts(Location other) {
      return this.equals(other);
    }
  }

  private static class Variable extends StringBasedObj {
    public Variable(String name) {
      super(name);
    }
  }

  private static class FieldWildCard extends FieldRef implements Wildcard {
    public FieldWildCard() {
      super("*");
    }
  }

  private static class ExclusionWildcardField extends FieldRef
      implements ExclusionWildcard<FieldRef> {
    private final FieldRef excludes;

    public ExclusionWildcardField(FieldRef excl) {
      super(excl.name);
      this.excludes = excl;
    }

    @Override
    public FieldRef excludes() {
      return (FieldRef) excludes;
    }

    @Override
    public String toString() {
      return "not " + super.toString();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((excludes == null) ? 0 : excludes.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      ExclusionWildcardField other = (ExclusionWildcardField) obj;
      if (excludes == null) {
        if (other.excludes != null) return false;
      } else if (!excludes.equals(other.excludes)) return false;
      return true;
    }
  }

  private static class FieldRef extends StringBasedObj implements Location {
    public FieldRef(String name) {
      super(name);
    }

    @Override
    public boolean accepts(Location other) {
      return this.equals(other);
    }
  }

  private static class StringBasedObj {
    final String name;

    public StringBasedObj(String name) {
      this.name = name;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      StringBasedObj other = (StringBasedObj) obj;
      if (name == null) {
        if (other.name != null) return false;
      } else if (!name.equals(other.name)) return false;
      return true;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
