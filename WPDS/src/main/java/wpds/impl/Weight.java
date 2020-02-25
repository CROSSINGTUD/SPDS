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
package wpds.impl;

public abstract class Weight {
  public abstract Weight extendWith(Weight other);

  public abstract Weight combineWith(Weight other);

  public static NoWeight NO_WEIGHT_ONE = new Weight.NoWeight();

  public static class NoWeight extends Weight {
    @Override
    public Weight extendWith(Weight other) {
      return other;
    }

    @Override
    public Weight combineWith(Weight other) {
      return other;
    }

    @Override
    public String toString() {
      return "";
    }
  }
}
