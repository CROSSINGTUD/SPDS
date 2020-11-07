package boomerang.guided.targets;

import java.io.File;

public class WrappedInNewStringInnerTarget {

  public static void main(String... args) {
    String x = "bar";
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
