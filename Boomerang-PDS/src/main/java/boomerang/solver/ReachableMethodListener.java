package boomerang.solver;

import soot.SootMethod;

public interface ReachableMethodListener {
	void reachable(AbstractBoomerangSolver solver, SootMethod m);
}
