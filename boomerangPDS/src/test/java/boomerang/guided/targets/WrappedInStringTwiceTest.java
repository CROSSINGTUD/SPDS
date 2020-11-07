package boomerang.guided.targets;

import java.io.File;

public class WrappedInStringTwiceTest {

  public static void main(String... args) {
    String x = new String("bar");
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
