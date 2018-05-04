package boomerang.callgraph;

public interface CallerListener<M, N> {
    void onCallerAdded(M m, N n);
}
