package boomerang.callgraph;

public interface CalleeListener<M, N> {
    void onCalleeAdded(M m, N n);
}
