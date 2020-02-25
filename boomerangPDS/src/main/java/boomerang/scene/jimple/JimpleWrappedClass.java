package boomerang.scene.jimple;

import boomerang.scene.Method;
import boomerang.scene.Type;
import boomerang.scene.WrappedClass;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import soot.SootClass;
import soot.SootMethod;

public class JimpleWrappedClass implements WrappedClass {

  private SootClass delegate;
  private Set<Method> methods;

  public JimpleWrappedClass(SootClass delegate) {
    this.delegate = delegate;
  }

  public Set<Method> getMethods() {
    List<SootMethod> ms = delegate.getMethods();
    if (methods == null) {
      methods = Sets.newHashSet();
      for (SootMethod m : ms) {
        if (m.hasActiveBody()) methods.add(JimpleMethod.of(m));
      }
    }
    return methods;
  }

  public boolean hasSuperclass() {
    return delegate.hasSuperclass();
  }

  public WrappedClass getSuperclass() {
    return new JimpleWrappedClass(delegate.getSuperclass());
  }

  public Type getType() {
    return new JimpleType(delegate.getType());
  }

  public boolean isApplicationClass() {
    return delegate.isApplicationClass();
  }

  @Override
  public String toString() {
    return delegate.toString();
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
    JimpleWrappedClass other = (JimpleWrappedClass) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    return true;
  }

  @Override
  public String getFullyQualifiedName() {
    return delegate.getName();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public Object getDelegate() {
    return delegate;
  }
}
