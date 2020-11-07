package boomerang.guided.targets;

import java.io.File;

public class BranchingAfterNewStringTest {

  public static void main(String... args) {
    String x = new String("bar");
    String y = new String("foo");
    String bar = doPassArgument(Math.random() > 0 ? x : y);
    new File(bar);
    ;
  }

  public static String doPassArgument(String param) {
    String x = new String(param);
    System.out.println(x);
    return x;
  }
}
