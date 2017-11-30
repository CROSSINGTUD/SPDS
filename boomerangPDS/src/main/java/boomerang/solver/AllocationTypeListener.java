package boomerang.solver;

import soot.RefType;
import soot.SootMethod;

public interface AllocationTypeListener {

	void allocationType(RefType m);

}
