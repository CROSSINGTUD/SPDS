/**
 * ***************************************************************************** Copyright (c) 2020
 * CodeShield GmbH, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.scene.wala;

import boomerang.scene.Method;
import boomerang.scene.Type;
import boomerang.scene.WrappedClass;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import java.util.Set;

public class WALAClass implements WrappedClass {

  private final TypeReference delegate;

  public WALAClass(TypeReference typeReference) {
    this.delegate = typeReference;
  }

  @Override
  public Set<Method> getMethods() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean hasSuperclass() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public WrappedClass getSuperclass() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Type getType() {
    return null;
  }

  @Override
  public boolean isApplicationClass() {
    return delegate.getClassLoader().equals(ClassLoaderReference.Application)
        || delegate.getClassLoader().equals(JavaSourceAnalysisScope.SOURCE);
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
    WALAClass other = (WALAClass) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    return true;
  }

  @Override
  public String getFullyQualifiedName() {
    if (delegate.getName() == null) return "FAILED";
    if (delegate.getName().getPackage() == null) {
      return getName();
    }
    return delegate.getName().getPackage().toString().replace("/", ".")
        + "."
        + delegate.getName().getClassName();
  }

  @Override
  public String getName() {
    return delegate.getName().getClassName().toString();
  }

  @Override
  public Object getDelegate() {
    return delegate;
  }
}
