package boomerang.scene.wala;

import boomerang.scene.DataFlowScope;
import boomerang.scene.DeclaredMethod;
import boomerang.scene.Method;

public class WALADataFlowScope {
  public static DataFlowScope make() {
    return new DataFlowScope() {
      @Override
      public boolean isExcluded(DeclaredMethod method) {
        return false;
      }

      @Override
      public boolean isExcluded(Method method) {
        return false;
      }
    };
  }

  public static DataFlowScope APPLICATION_ONLY =
      new DataFlowScope() {
        @Override
        public boolean isExcluded(DeclaredMethod method) {
          return !method.getDeclaringClass().isApplicationClass() || method.isNative();
        }

        @Override
        public boolean isExcluded(Method method) {
          return !method.getDeclaringClass().isApplicationClass() || method.isNative();
        }
      };
}
