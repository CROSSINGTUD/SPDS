package boomerang.shared.context.targets;

import java.io.File;

public class SharedContextTarget4 {

  public static void main(String...args){
    String bar = doPassArgument("bar");
    new File(bar);;
  }

  public static String doPassArgument(String param) {
    String x = new String(param);
    System.out.println(x);
    return  x;
  }
}
