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

import boomerang.scene.DeclaredMethod;
import boomerang.scene.InvokeExpr;
import boomerang.scene.WrappedClass;
import com.ibm.wala.types.MethodReference;

public class WALADeclaredMethod extends DeclaredMethod {

  private final MethodReference delegate;
  private final boolean isStatic;

  public WALADeclaredMethod(InvokeExpr inv, MethodReference ref) {
    super(inv);
    this.delegate = ref;
    this.isStatic = inv.isStaticInvokeExpr();
  }

  @Override
  public boolean isNative() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getSubSignature() {
    return delegate.getName().toString();
  }

  @Override
  public String getName() {
    return delegate.getName().toString();
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public boolean isConstructor() {
    return delegate.isInit();
  }

  @Override
  public String getSignature() {
    return delegate.getSignature();
  }

  @Override
  public WrappedClass getDeclaringClass() {
    return new WALAClass(delegate.getDeclaringClass());
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
    WALADeclaredMethod other = (WALADeclaredMethod) obj;
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
