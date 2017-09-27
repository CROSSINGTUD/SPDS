package boomerang.solver;

import soot.SootMethod;
import wpds.impl.Weight;

public interface ReachableMethodListener<W extends Weight> {
	void reachable(AbstractBoomerangSolver<W> solver, SootMethod m);
}
