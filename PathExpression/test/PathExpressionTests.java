import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pathexpression.PathExpression;
import pathexpression.PathExpressionComputer;
import pathexpression.RegEx;


public class PathExpressionTests {
  @Test
  public void simple() {
    IntGraph g = new IntGraph();
    g.addEdge(1, "w", 2);
    PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
    PathExpression<Integer, String> expressionBetween = expr.getExpressionBetween(1, 2);
    PathExpression<Integer, String> expected =
        new PathExpression<Integer, String>(new RegEx.Plain<String>("w"), 1, 2);
    assertEquals(expected, expressionBetween);
  }

  @Test
  public void simple2() {
    IntGraph g = new IntGraph();
    g.addEdge(1, "w", 2);
    g.addEdge(2, "w", 3);
    PathExpressionComputer<Integer, String> expr = new PathExpressionComputer<Integer, String>(g);
    PathExpression<Integer, String> expressionBetween = expr.getExpressionBetween(1, 3);
    PathExpression<Integer, String> expected =
        new PathExpression<Integer, String>(new RegEx.Plain<String>("ww"), 1, 3);
    assertEquals(expected, expressionBetween);
  }
}
