package boomerang.scene.jimple;

import boomerang.scene.AllocVal;
import boomerang.scene.Type;
import boomerang.scene.Val;
import boomerang.scene.WrappedClass;
import soot.ArrayType;
import soot.BooleanType;
import soot.NullType;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;

public class JimpleType implements Type {

  private soot.Type delegate;

  public JimpleType(soot.Type type) {
    this.delegate = type;
  }

  public boolean isNullType() {
    return delegate instanceof NullType;
  }

  public boolean isRefType() {
    return delegate instanceof RefType;
  }

  @Override
  public boolean isBooleanType() {
    return delegate instanceof BooleanType;
  }

  public boolean isArrayType() {
    return delegate instanceof ArrayType;
  }

  public Type getArrayBaseType() {
    return new JimpleType(((ArrayType) delegate).baseType);
  }

  public WrappedClass getWrappedClass() {
    return new JimpleWrappedClass(((RefType) delegate).getSootClass());
  }

  public soot.Type getDelegate() {
    return delegate;
  }

  @Override
  public boolean doesCastFail(Type targetVal, Val target) {
    RefType targetType = (RefType) ((JimpleType) targetVal).getDelegate();
    RefType sourceType = (RefType) this.getDelegate();
    if (targetType.getSootClass().isPhantom() || sourceType.getSootClass().isPhantom())
      return false;
    if (target instanceof AllocVal && ((AllocVal) target).getAllocVal().isNewExpr()) {
      boolean castFails = Scene.v().getOrMakeFastHierarchy().canStoreType(targetType, sourceType);
      return !castFails;
    }
    // TODO this line is necessary as canStoreType does not properly work for
    // interfaces, see Java doc.
    if (targetType.getSootClass().isInterface()) {
      return false;
    }
    boolean castFails =
        Scene.v().getOrMakeFastHierarchy().canStoreType(targetType, sourceType)
            || Scene.v().getOrMakeFastHierarchy().canStoreType(sourceType, targetType);
    return !castFails;
  }

  public boolean isSubtypeOf(String type) {
    SootClass interfaceType = Scene.v().getSootClass(type);
    if (delegate.toString().equals(type)) return true;
    if (!(delegate instanceof RefType)) {
      if (delegate instanceof ArrayType) {
        return true;
      }
      if (delegate instanceof PrimType) {
        return type.equals(delegate.toString());
      }
      throw new RuntimeException("More");
    }

    RefType allocatedType = (RefType) delegate;
    if (!interfaceType.isInterface()) {
      return Scene.v().getFastHierarchy().isSubclass(allocatedType.getSootClass(), interfaceType);
    }
    if (Scene.v()
        .getActiveHierarchy()
        .getSubinterfacesOfIncluding(interfaceType)
        .contains(allocatedType.getSootClass())) return true;
    return Scene.v()
        .getActiveHierarchy()
        .getImplementersOf(interfaceType)
        .contains(allocatedType.getSootClass());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    JimpleType other = (JimpleType) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    return true;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
