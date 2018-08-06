package boomerang.seedfactory;

import boomerang.Query;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

import java.util.*;

public abstract class SimpleSeedFactory {

    protected ObservableICFG<Unit,SootMethod> icfg;

    public SimpleSeedFactory(ObservableICFG<Unit,SootMethod> icfg) {
        this.icfg = icfg;
    }

    public Collection<Query> computeSeeds() {
        List<Query> seeds = new ArrayList<>();

        List<SootMethod> worklist = new ArrayList<>();
        worklist.addAll(Scene.v().getEntryPoints());

        Set<SootMethod> visited = new HashSet<>();
        while (!worklist.isEmpty()){
            SootMethod m = worklist.get(0);
            visited.add(m);
            worklist.remove(m);
            if(!m.hasActiveBody())
                continue;
            for(Unit u : m.getActiveBody().getUnits()) {
                seeds.addAll(generate(m, (Stmt) u));
                if (icfg.isCallStmt(u)) {
                    icfg.addCalleeListener(new CalleeListener<Unit, SootMethod>() {
                        @Override
                        public Unit getObservedCaller() {
                            return u;
                        }

                        @Override
                        public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
                            if (!visited.contains(sootMethod) && !worklist.contains(sootMethod)){
                                worklist.add(sootMethod);
                            }
                        }
                    });
                }
            }
        }
        return seeds;
    }

    protected abstract Collection<? extends Query> generate(SootMethod method, Stmt u);

}
