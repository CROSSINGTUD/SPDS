package boomerang.scene;

public abstract class DeclaredMethod {

  private InvokeExpr inv;

  public DeclaredMethod(InvokeExpr inv) {
    this.inv = inv;
  }

  public abstract boolean isNative();

  public abstract String getSubSignature();

  public abstract String getName();

  public abstract boolean isStatic();

  public abstract boolean isConstructor();

  public abstract String getSignature();

  public abstract WrappedClass getDeclaringClass();

  public InvokeExpr getInvokeExpr() {
    return inv;
  }
}
