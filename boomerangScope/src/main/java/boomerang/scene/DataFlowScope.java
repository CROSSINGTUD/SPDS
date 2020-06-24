package boomerang.scene;

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
