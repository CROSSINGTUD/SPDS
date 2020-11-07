package boomerang.guided.targets;

import java.io.File;

public class ContextSensitiveTarget {

  public static void main(String... args) {
    String bar = doPassArgument("bar");
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
