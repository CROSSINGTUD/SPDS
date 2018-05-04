package boomerang.callgraph;

public interface CallerListener<N, M> {
    void onCallerAdded(N n, M m);
}
