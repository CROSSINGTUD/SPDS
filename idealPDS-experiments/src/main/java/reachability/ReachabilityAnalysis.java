package reachability;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import soot.SootMethod;
import soot.Unit;

public class ReachabilityAnalysis {
	final ObservableICFG<Unit, SootMethod> icfg;
	
	public ReachabilityAnalysis(ObservableICFG<Unit, SootMethod> icfg) {
		this.icfg = icfg;
	}
	

	public Set<SootMethod> reachbleFrom(Collection<SootMethod> entryMethods) {
		LinkedList<SootMethod> worklist = new LinkedList<>(entryMethods);
		Set<SootMethod> visited = new HashSet<>();
		while (!worklist.isEmpty()) {
			SootMethod m = worklist.pop();
			visited.add(m);
			if (!m.hasActiveBody())
				continue;
			for (Unit u : m.getActiveBody().getUnits()) {
				if (icfg.isCallStmt(u)) {
					icfg.addCalleeListener(new CalleeListener<Unit, SootMethod>() {
						@Override
						public Unit getObservedCaller() {
							return u;
						}

						@Override
						public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
							if (!visited.contains(sootMethod) && !worklist.contains(sootMethod)) {
								System.out.println(sootMethod);
								worklist.add(sootMethod);
							}
						}
					});
				}
			}
		}
		return visited;
	}
}
