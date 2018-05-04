package boomerang.callgraph;

public interface CallerObserver {
    //TODO this needs parameters, probably the new caller and its context
    void onCallerAdded();
}
