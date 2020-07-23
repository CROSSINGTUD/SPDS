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

import boomerang.scene.Type;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;

public class WALADummyVal extends WALAVal {

  public WALADummyVal(WALAMethod method) {
    super(-1, method);
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public Type getType() {
    return new WALAType(TypeAbstraction.TOP);
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  @Override
  public String toString() {
    return "dummy in " + method;
  }
}
