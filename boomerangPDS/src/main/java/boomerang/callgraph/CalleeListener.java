package boomerang.callgraph;

public interface CalleeListener<N, M> {

    N getObservedCaller();

    void onCalleeAdded(N n, M m);
}
