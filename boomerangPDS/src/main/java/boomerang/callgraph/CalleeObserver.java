package boomerang.callgraph;

public interface CalleeObserver {
    //TODO this needs parameters, probably the new callee and its context
    void onCalleeAdded();
}
