package boomerang.scene;

import java.util.Set;

public interface WrappedClass {

  public Set<Method> getMethods();

  public boolean hasSuperclass();

  public WrappedClass getSuperclass();

  public Type getType();

  public boolean isApplicationClass();

  public String getFullyQualifiedName();

  public String getName();

  public Object getDelegate();
}
