package boomerang.callgraph;

import boomerang.WeightedBoomerang;
import boomerang.scene.CallGraph;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import java.util.Collection;

public interface ICallerCalleeResolutionStrategy {

  interface Factory {
    ICallerCalleeResolutionStrategy newInstance(WeightedBoomerang solver, CallGraph cg);
  }

  void computeFallback(ObservableDynamicICFG observableDynamicICFG);

  Method resolveSpecialInvoke(InvokeExpr ie);

  Collection<Method> resolveInstanceInvoke(Statement stmt);

  Method resolveStaticInvoke(InvokeExpr ie);
}
