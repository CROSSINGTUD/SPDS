package boomerang.scene;

public interface Type {

  boolean isNullType();

  boolean isRefType();

  boolean isArrayType();

  Type getArrayBaseType();

  WrappedClass getWrappedClass();

  boolean doesCastFail(Type targetVal, Val target);

  boolean isSubtypeOf(String type);

  boolean isBooleanType();
}
