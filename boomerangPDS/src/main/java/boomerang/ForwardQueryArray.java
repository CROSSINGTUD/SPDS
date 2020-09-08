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
package boomerang;

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Val;
import com.google.common.base.Objects;

public class ForwardQueryArray extends ForwardQuery {

  private final Integer index;

  public ForwardQueryArray(Edge stmt, Val variable, Integer index) {
    super(stmt, variable);
    this.index = index;
  }

  @Override
  public String toString() {
    return "ArrayForwardQuery: " + super.toString() + " Index: " + index;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!super.equals(o)) {
      return false;
    }
    ForwardQueryArray that = (ForwardQueryArray) o;
    return Objects.equal(index, that.index);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), index);
  }

  public Integer getIndex() {
    return index;
  }
}
