package boomerang;

import boomerang.scene.DeclaredMethod;
import boomerang.scene.Method;

public interface DataFlowScope {

  DataFlowScope INCLUDE_ALL =
      new DataFlowScope() {
        @Override
        public boolean isExcluded(DeclaredMethod method) {
          return false;
        }

        @Override
        public boolean isExcluded(Method method) {
          return false;
        }
      };

  public boolean isExcluded(DeclaredMethod method);

  public boolean isExcluded(Method method);
}
