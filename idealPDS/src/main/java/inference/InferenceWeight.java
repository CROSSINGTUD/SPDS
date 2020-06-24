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
package inference;

import boomerang.scene.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import wpds.impl.Weight;

public class InferenceWeight extends Weight {

  private final Set<Method> invokedMethods;
  private final String rep;
  private static InferenceWeight one;
  private static InferenceWeight zero;

  private InferenceWeight(String rep) {
    this.rep = rep;
    this.invokedMethods = null;
  }

  private InferenceWeight(Set<Method> res) {
    this.invokedMethods = res;
    this.rep = null;
  }

  public InferenceWeight(Method m) {
    this.invokedMethods = Collections.singleton(m);
    this.rep = null;
  }

  @Override
  public Weight extendWith(Weight other) {
    if (other.equals(one())) return this;
    if (this.equals(one())) return other;
    if (other.equals(zero()) || this.equals(zero())) {
      return zero();
    }
    InferenceWeight func = (InferenceWeight) other;
    Set<Method> otherInvokedMethods = func.invokedMethods;
    Set<Method> res = new HashSet<>(invokedMethods);
    res.addAll(otherInvokedMethods);
    return new InferenceWeight(res);
  }

  @Override
  public Weight combineWith(Weight other) {
    return extendWith(other);
  }

  public static InferenceWeight one() {
    if (one == null) one = new InferenceWeight("ONE");
    return one;
  }

  public static InferenceWeight zero() {
    if (zero == null) zero = new InferenceWeight("ZERO");
    return zero;
  }

  public String toString() {
    if (this.rep != null) return this.rep;
    return "{Func:" + invokedMethods.toString() + "}";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    result = prime * result + ((invokedMethods == null) ? 0 : invokedMethods.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    InferenceWeight other = (InferenceWeight) obj;
    if (rep == null) {
      if (other.rep != null) return false;
    } else if (!rep.equals(other.rep)) return false;
    if (invokedMethods == null) {
      if (other.invokedMethods != null) return false;
    } else if (!invokedMethods.equals(other.invokedMethods)) return false;
    return true;
  }
}
