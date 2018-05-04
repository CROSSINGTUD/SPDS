package boomerang.callgraph;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class ObservableICFGImpl implements ObservableICFG<Unit, SootMethod>{

    //Add enable exceptions flag like in JimpleBasedICFG?
    public ObservableICFGImpl(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
    }

    public ObservableICFGImpl(){

    }

    public ObservableICFGImpl(ObservableICFG<Unit,SootMethod> icfg) {
    }
}
