package boomerang.guided.targets;

import java.io.File;

public class PingPongInterproceduralTarget {

  public static void main(String... args) {
    StringBuilder sb = new StringBuilder();
    final String result = doCreateFileName(sb);
    File file = new File(result);
  }

  private static String doCreateFileName(StringBuilder sb) {
    sb.append("hello");
    appendMe(sb, "world");
    return sb.toString();
  }

  private static void appendMe(StringBuilder sb, String world) {
    sb.append(world);
  }
}
