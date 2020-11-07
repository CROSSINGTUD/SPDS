package boomerang.guided.targets;

import java.io.File;

public class PingPongTarget {

  public static void main(String... args) {
    final StringBuilder sb = new StringBuilder();
    final String hello = "hello";
    sb.append(hello).append("world");
    final String result = sb.toString();
    File file = new File(result);
  }
}
