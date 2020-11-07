package boomerang.guided.targets;

import java.io.File;

public class BranchingTest {

  public static void main(String... args) {
    String x = new String(Math.random() > 0 ? "bar" : "foo");
    String bar = doPassArgument(x);
    new File(bar);
    ;
  }

  public static String doPassArgument(String param) {
    String x = new String(param);
    System.out.println(x);
    return x;
  }
}
