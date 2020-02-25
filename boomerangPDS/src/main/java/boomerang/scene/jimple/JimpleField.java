/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.scene.jimple;

import boomerang.scene.Field;
import soot.SootField;

public class JimpleField extends Field {
  private final SootField delegate;

  public JimpleField(SootField delegate) {
    super();
    this.delegate = delegate;
  }

  @Override
  public String toString() {
    return delegate.getName().toString();
  }

  public SootField getSootField() {
    return this.delegate;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    JimpleField other = (JimpleField) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    return true;
  }

  @Override
  public boolean isInnerClassField() {
    return this.delegate.getName().contains("$");
  }
}
