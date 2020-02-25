package boomerang.callgraph;

public interface CalleeListener<N, M> {

  N getObservedCaller();

  void onCalleeAdded(N callSite, M callee);

  void onNoCalleeFound();
}
