package test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pathexpression.PathExpressionComputer;
import pathexpression.RegEx;


public class PathExpressionTests {
  @Test
  public void simple() {
    IntGraph g = new IntGraph();
    g.addEdge(1, "w", 2);
    PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
    RegEx<String> expressionBetween = expr.getExpressionBetween(1, 2);
    RegEx<String> expected = new RegEx.Plain<String>("w");
    assertEquals(expected, expressionBetween);
  }

  @Test
  public void simple2() {
    IntGraph g = new IntGraph();
    g.addEdge(1, "w", 2);
    g.addEdge(2, "w", 3);
    PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
    RegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
    RegEx<String> expected = new RegEx.Plain<String>("ww");
    assertEquals(expected, expressionBetween);
  }

  @Test
  public void star() {
    IntGraph g = new IntGraph();
    g.addEdge(1, "a", 2);
    g.addEdge(2, "b", 2);
    g.addEdge(2, "c", 3);
    PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
    RegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
    RegEx<String> expected = a(a("a", star("b")), "c");
    assertEquals(expected, expressionBetween);
  }

  @Test
  public void union() {
    IntGraph g = new IntGraph();
    g.addEdge(1, "a", 2);
    g.addEdge(2, "b", 3);
    g.addEdge(1, "c", 4);
    g.addEdge(4, "d", 3);
    PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
    RegEx<String> expressionBetween = expr.getExpressionBetween(1, 3);
    RegEx<String> expected = u(a("a", "b"), a("c", "d"));
    assertEquals(expected, expressionBetween);
  }

  private static RegEx<String> e(String e) {
    return new RegEx.Plain<String>(e);
  }

  private static RegEx<String> a(RegEx<String> a, RegEx<String> b) {
    return RegEx.<String>concatenate(a, b);
  }

  private static RegEx<String> a(String a, RegEx<String> b) {
    return a(e(a), b);
  }

  private static RegEx<String> a(RegEx<String> a, String b) {
    return a(a, e(b));
  }

  private static RegEx<String> a(String a, String b) {
    return a(e(a), e(b));
  }

  private static RegEx<String> u(RegEx<String> a, RegEx<String> b) {
    return RegEx.<String>union(a, b);
  }

  private static RegEx<String> u(String a, String b) {
    return u(e(a), e(b));
  }

  private static RegEx<String> u(RegEx<String> a, String b) {
    return u(a, e(b));
  }

  private static RegEx<String> u(String a, RegEx<String> b) {
    return u(e(a), b);
  }

  private static RegEx<String> star(RegEx<String> a) {
    return RegEx.<String>star(a);
  }

  private static RegEx<String> star(String a) {
    return star(e(a));
  }
}
