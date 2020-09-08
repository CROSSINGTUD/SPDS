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

public class ForwardQueryMultiDimensionalArray extends ForwardQueryArray {

  private final Integer index2;

  public ForwardQueryMultiDimensionalArray(
      Edge stmt, Val variable, Integer index1, Integer index2) {
    super(stmt, variable, index1);
    this.index2 = index2;
  }

  @Override
  public String toString() {
    return "ArrayForwardQuery: "
        + super.toString()
        + " Index1: "
        + getIndex1()
        + " Index2: "
        + index2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!super.equals(o)) {
      return false;
    }
    ForwardQueryMultiDimensionalArray that = (ForwardQueryMultiDimensionalArray) o;
    return Objects.equal(index2, that.index2);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), index2);
  }

  public Integer getIndex1() {
    return getIndex();
  }

  public Integer getIndex2() {
    return index2;
  }
}
