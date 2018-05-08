package boomerang.callgraph;

public interface CallListener<N, M> {
    void onCallAdded(N n, M m);
}
