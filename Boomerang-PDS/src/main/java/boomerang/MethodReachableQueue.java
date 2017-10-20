package boomerang;

import soot.SootMethod;

public interface MethodReachableQueue {

	void submit(SootMethod method, Runnable runnable);

}
