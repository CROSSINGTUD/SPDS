package boomerang.callgraph;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class CallGraphExtractor implements CallListener<Unit,SootMethod> {

    //TODO store call graph to the best of your knowledge
    private CallGraph callGraph = new CallGraph();
    private ObservableICFG<Unit,SootMethod> icfg;

    public CallGraphExtractor(ObservableICFG<Unit,SootMethod> icfg){
        this.icfg = icfg;
    }

    @Override
    public void onCallAdded(Unit unit, SootMethod sootMethod) {
        //TODO add edge to call graph
    }
}
