package boomerang.callgraph;

public interface CalleeListener<N, M> {
    void onCalleeAdded(N n, M m);
}
