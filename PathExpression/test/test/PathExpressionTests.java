/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pathexpression.IRegEx;
import pathexpression.PathExpressionComputer;
import pathexpression.RegEx;


public class PathExpressionTests {
  // @Test
  // public void simple() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, "w", 2);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 2);
  // IRegEx<String> expected = new RegEx.Plain<String>("w");
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void simple2() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, "w", 2);
  // g.addEdge(2, "w", 3);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
  // IRegEx<String> expected = a("w", "w");
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void simple3() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, "a", 2);
  // g.addEdge(2, "b", 3);
  // g.addEdge(3, "c", 4);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 4);
  // IRegEx<String> expected = a(a("a", "b"), "c");
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void star2() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, "a", 2);
  // g.addEdge(2, "b", 1);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 2);
  // IRegEx<String> expected = a("a", star(a("b", "a")));
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void star3() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, "a", 2);
  // g.addEdge(2, "b", 1);
  // g.addEdge(1, "c", 2);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 2);
  // IRegEx<String> expected = a(u("a", "c"), star(a("b", u("a", "c"))));
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void simple4() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, "a", 2);
  // g.addEdge(2, "b", 3);
  // g.addEdge(3, "c", 4);
  // g.addEdge(1, "c", 4);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 4);
  // IRegEx<String> expected = u("c", a(a("a", "b"), "c"));
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void star() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, "a", 2);
  // g.addEdge(2, "b", 2);
  // g.addEdge(2, "c", 3);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
  // IRegEx<String> expected = a(a("a", star("b")), "c");
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void union() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, "a", 2);
  // g.addEdge(2, "b", 3);
  // g.addEdge(1, "c", 4);
  // g.addEdge(4, "d", 3);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
  // IRegEx<String> expected = u(a("a", "b"), a("c", "d"));
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void empty() {
  // IntGraph g = new IntGraph();
  // g.addEdge(2, "a", 1);
  // g.addEdge(2, "b", 3);
  // g.addEdge(3, "c", 1);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
  // IRegEx<String> expected = new RegEx.EmptySet<String>();
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void unionAndConcatenate() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, "a", 2);
  // g.addEdge(2, "b", 4);
  // g.addEdge(1, "a", 3);
  // g.addEdge(3, "b", 4);
  // g.addEdge(4, "c", 5);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 5);
  // IRegEx<String> expected = a(a("a", "b"), "c");
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void empty2() {
  // IntGraph g = new IntGraph();
  // g.addEdge(3, "c", 1);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
  // IRegEx<String> expected = new RegEx.EmptySet<String>();
  // assertEquals(expected, expressionBetween);
  // }
  // @Test
  // public void epsilon() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, g.epsilon(), 3);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
  // IRegEx<String> expected = expr.getEpsilon();
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void epsilon2() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, g.epsilon(), 2);
  // g.addEdge(2, g.epsilon(), 3);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
  // IRegEx<String> expected = expr.getEpsilon();
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void epsilon3() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, g.epsilon(), 2);
  // g.addEdge(2, g.epsilon(), 3);
  // g.addEdge(1, g.epsilon(), 4);
  // g.addEdge(4, g.epsilon(), 3);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
  // IRegEx<String> expected = expr.getEpsilon();
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void epsilon5() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, g.epsilon(), 2);
  // g.addEdge(2, g.epsilon(), 3);
  // g.addEdge(1, g.epsilon(), 4);
  // g.addEdge(4, g.epsilon(), 3);
  // g.addEdge(3, "a", 5);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 5);
  // IRegEx<String> expected = new RegEx.Plain<String>("a");
  // assertEquals(expected, expressionBetween);
  // }
  //
  // @Test
  // public void branchWithEps() {
  // IntGraph g = new IntGraph();
  // g.addEdge(1, "a", 2);
  // g.addEdge(2, "v", 3);
  // g.addEdge(1, "c", 3);
  // g.addEdge(3, g.epsilon(), 4);
  // PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
  // IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 4);
  // IRegEx<String> expected = u("c", a("a", "v"));
  // assertEquals(expected, expressionBetween);
  // }

  @Test
  public void branchWithEps2() {
    IntGraph g = new IntGraph();
    g.addEdge(1, "a", 2);
    g.addEdge(2, "v", 4);
    g.addEdge(1, "c", 3);
    g.addEdge(3, g.epsilon(), 4);
    PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
    IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 4);
    IRegEx<String> expected = u("c", a("a", "v"));
    assertEquals(expected, expressionBetween);
  }

  @Test
  public void branchWithEps3() {
    IntGraph g = new IntGraph();
    g.addEdge(1, "a", 2);
    g.addEdge(2, "v", 3);
    g.addEdge(1, "c", 4);
    g.addEdge(4, g.epsilon(), 3);
    PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
    IRegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
    IRegEx<String> expected = u("c", a("a", "v"));
    assertEquals(expected, expressionBetween);
  }

  @Test
  public void simpleReverse() {
    IntGraph g = new IntGraph();
    g.addEdge(3, "a", 2);
    g.addEdge(2, "v", 1);
    PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
    IRegEx<String> expressionBetween = expr.getExpressionBetween(3, 1);
    IRegEx<String> expected = a("a", "v");
    assertEquals(expected, expressionBetween);
  }
  private static IRegEx<String> e(String e) {
    return new RegEx.Plain<String>(e);
  }


  private static IRegEx<String> a(IRegEx<String> a, IRegEx<String> b) {
    return RegEx.<String>concatenate(a, b);
  }

  private static IRegEx<String> a(String a, IRegEx<String> b) {
    return a(e(a), b);
  }

  private static IRegEx<String> a(IRegEx<String> a, String b) {
    return a(a, e(b));
  }

  private static IRegEx<String> a(String a, String b) {
    return a(e(a), e(b));
  }

  private static IRegEx<String> u(IRegEx<String> a, IRegEx<String> b) {
    return RegEx.<String>union(a, b);
  }

  private static IRegEx<String> u(String a, String b) {
    return u(e(a), e(b));
  }

  private static IRegEx<String> u(IRegEx<String> a, String b) {
    return u(a, e(b));
  }

  private static IRegEx<String> u(String a, IRegEx<String> b) {
    return u(e(a), b);
  }

  private static IRegEx<String> star(IRegEx<String> a) {
    return RegEx.<String>star(a);
  }

  private static IRegEx<String> star(String a) {
    return star(e(a));
  }
}
