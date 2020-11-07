package boomerang.guided.targets;

import java.io.File;

public class ContextSensitiveAndLeftUnbalancedTarget {

  public static void main(String... args) {
    context("bar");
  }

  private static void context(String barParam) {
    String bar = doPassArgument(barParam);
    String foo = doPassArgument("foo");
    String quz = doPassArgument("quz");
    new File(bar);
    new File(foo);
    new File(quz);
  }

  private static String doPassArgument(String param) {
    return new String(param);
  }
}
