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
package tests;

import wpds.impl.Weight;
import wpds.interfaces.Location;

public class NumWeight extends Weight {

  private int i;

  public NumWeight(int i) {
    this.i = i;
  }

  private NumWeight() {}

  @Override
  public Weight extendWith(Weight other) {
    if (this.equals(one())) return other;
    if (other.equals(one())) return this;
    if (this.equals(zero()) || other.equals(zero())) return zero();
    NumWeight o = (NumWeight) other;
    return new NumWeight(o.i + i);
  }

  @Override
  public Weight combineWith(Weight other) {
    if (other.equals(zero())) return this;
    if (this.equals(zero())) return other;
    NumWeight o = (NumWeight) other;
    if (o.i == i) return o;
    return zero();
  }

  private static NumWeight one;

  public static <N extends Location> NumWeight one() {
    if (one == null)
      one =
          new NumWeight() {
            @Override
            public String toString() {
              return "<ONE>";
            }

            @Override
            public boolean equals(Object obj) {
              return obj == this;
            }
          };
    return one;
  }

  private static NumWeight zero;

  public static <N extends Location> NumWeight zero() {
    if (zero == null)
      zero =
          new NumWeight() {
            @Override
            public String toString() {
              return "<ZERO>";
            }

            @Override
            public boolean equals(Object obj) {
              return obj == this;
            }
          };
    return zero;
  }

  @Override
  public String toString() {
    return Integer.toString(i);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + i;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    NumWeight other = (NumWeight) obj;
    if (i != other.i) return false;
    return true;
  }
}
