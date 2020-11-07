package boomerang.guided.targets;

import java.io.File;

public class NestedContextAndBranchingTarget {

  public static void main(String... args) {
    String bar = doPassArgument("bar");
    new File(bar);
  }

  private static String doPassArgument(String level0) {
    return wrappedWayDeeper(new String(level0));
  }

  private static String wrappedWayDeeper(String level1) {
    if (Math.random() > 0) {
      return "foo";
    }
    return andMoreStacks(level1);
  }

  private static String andMoreStacks(String level2) {
    return level2;
  }
}
