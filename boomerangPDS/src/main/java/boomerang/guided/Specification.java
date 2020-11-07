package boomerang.guided;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Specification {
  private static final String ON_SELECTOR = "ON";
  private static final String GO_SELECTOR = "GO";
  private static final String BACKWARD = "{B}";
  private static final String FORWARD = "{F}";

  public enum QueryDirection {
    FORWARD,
    BACKWARD
  }

  private final Set<SootMethodWithSelector> methodAndQueries;

  private Specification(Collection<String> spec) {
    methodAndQueries = spec.stream().map(x -> parse(x)).collect(Collectors.toSet());
  }

  private SootMethodWithSelector parse(String input) {
    Pattern arguments = Pattern.compile("\\((.*?)\\)");
    Matcher argumentMatcher = arguments.matcher(input);
    Set<QuerySelector> on = Sets.newHashSet();
    Set<QuerySelector> go = Sets.newHashSet();

    // Handle arguments
    if (argumentMatcher.find()) {
      String group = argumentMatcher.group(1);
      String[] args = group.split(",");
      for (int i = 0; i < args.length; i++) {
        createQuerySelector(args[i], Parameter.of(i), on, go);
      }
    }

    // Handle base variable
    Pattern base = Pattern.compile("<(.*?):");
    Matcher baseMatcher = base.matcher(input);
    if (baseMatcher.find()) {
      String group = baseMatcher.group(1);
      createQuerySelector(group, Parameter.base(), on, go);
    }

    // Handle return
    String[] s = input.split(" ");
    createQuerySelector(s[1], Parameter.returnParam(), on, go);

    String sootMethod =
        input
            .replace(FORWARD, "")
            .replace(BACKWARD, "")
            .replace(ON_SELECTOR, "")
            .replace(GO_SELECTOR, "");

    // Assert parsing successful
    long backwardQueryCount =
        on.stream().filter(x -> x.direction == QueryDirection.BACKWARD).count()
            + go.stream().filter(x -> x.direction == QueryDirection.BACKWARD).count();
    long forwardQueryCount =
        on.stream().filter(x -> x.direction == QueryDirection.FORWARD).count()
            + go.stream().filter(x -> x.direction == QueryDirection.FORWARD).count();
    if (input.length()
        != sootMethod.length()
            + (on.size() * ON_SELECTOR.length()
                + go.size() * GO_SELECTOR.length()
                + backwardQueryCount * BACKWARD.length()
                + forwardQueryCount * FORWARD.length())) {
      throw new RuntimeException("Parsing Specification failed. Please check your specification");
    }

    return new SootMethodWithSelector(sootMethod, on, go);
  }

  private void createQuerySelector(
      String arg, Parameter p, Set<QuerySelector> on, Set<QuerySelector> go) {
    if (arg.startsWith(ON_SELECTOR)) {
      on.add(
          new QuerySelector(
              arg.contains(FORWARD) ? QueryDirection.FORWARD : QueryDirection.BACKWARD, p));
    }
    if (arg.startsWith(GO_SELECTOR)) {
      go.add(
          new QuerySelector(
              arg.contains(FORWARD) ? QueryDirection.FORWARD : QueryDirection.BACKWARD, p));
    }
  }

  public static class SootMethodWithSelector {
    SootMethodWithSelector(
        String sootMethod, Collection<QuerySelector> on, Collection<QuerySelector> go) {
      this.sootMethod = sootMethod;
      this.on = on;
      this.go = go;
    }

    private String sootMethod;
    private Collection<QuerySelector> on;
    private Collection<QuerySelector> go;

    public Collection<QuerySelector> getOn() {
      return on;
    }

    public Collection<QuerySelector> getGo() {
      return go;
    }

    public String getSootMethod() {
      return sootMethod;
    }
  }

  public static class Parameter {
    private final int value;

    private Parameter(final int newValue) {
      value = newValue;
    }

    public static Parameter returnParam() {
      return new Parameter(-2);
    }

    public static Parameter base() {
      return new Parameter(-1);
    }

    public static Parameter of(int integer) {
      if (integer < 0) {
        throw new RuntimeException("Parameter index must be > 0");
      }
      return new Parameter(integer);
    }

    public int getValue() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Parameter parameter = (Parameter) o;
      return value == parameter.value;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(value);
    }
  }

  public class QuerySelector {
    QuerySelector(QueryDirection direction, Parameter argumentSelection) {
      this.direction = direction;
      this.argumentSelection = argumentSelection;
    }

    QueryDirection direction;
    Parameter argumentSelection;
  }

  public static Specification loadFrom(String filePath) throws IOException {
    return new Specification(Files.lines(Paths.get(filePath)).collect(Collectors.toSet()));
  }

  public static Specification create(String... spec) {
    return new Specification(Sets.newHashSet(spec));
  }

  public Set<SootMethodWithSelector> getMethodAndQueries() {
    return methodAndQueries;
  }
}
