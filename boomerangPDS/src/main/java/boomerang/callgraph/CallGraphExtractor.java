package boomerang.callgraph;

import soot.SootMethod;
import soot.Unit;

public class CallGraphExtractor implements CalleeListener<Unit, SootMethod>, CallerListener<Unit,SootMethod> {

    //TODO store call graph to the best of your knowledge

    @Override
    public void onCalleeAdded(Unit unit, SootMethod sootMethod) {

    }

    @Override
    public void onCallerAdded(Unit unit, SootMethod sootMethod) {

    }
}
