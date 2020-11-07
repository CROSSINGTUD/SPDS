package boomerang.guided.targets;

import java.io.File;

public class LeftUnbalancedTarget {

  public static void main(String... args) {
    bar("bar");
  }

  private static void bar(String param) {
    String x = new String(param);
    File file = new File(new String(param));
  }
}
