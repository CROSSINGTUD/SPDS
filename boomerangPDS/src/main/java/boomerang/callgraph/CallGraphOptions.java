package boomerang.callgraph;

public class CallGraphOptions {
  boolean fallbackOnPrecomputedForUnbalanced() {
    return true;
  }

  boolean fallbackOnPrecomputedOnEmpty() {
    return false;
  }
}
