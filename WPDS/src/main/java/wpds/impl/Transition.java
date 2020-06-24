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

import pathexpression.Edge;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.wildcard.Wildcard;

public class Transition<N extends Location, D extends State> implements Edge<D, N> {
  private final D s1;
  private final N l1;
  private final D s2;
  private int hashCode;

  public Transition(D s1, N l1, D s2) {
    assert s1 != null;
    assert s2 != null;
    assert l1 != null;
    this.s1 = s1;
    this.l1 = l1;
    this.s2 = s2;
    if (l1 instanceof Wildcard) throw new RuntimeException("No wildcards allowed!");
  }

  public Configuration<N, D> getStartConfig() {
    return new Configuration<N, D>(l1, s1);
  }

  public D getTarget() {
    return s2;
  }

  public D getStart() {
    return s1;
  }

  @Override
  public int hashCode() {
    if (hashCode != 0) return hashCode;
    final int prime = 31;
    int result = 1;
    result = prime * result + ((l1 == null) ? 0 : l1.hashCode());
    result = prime * result + ((s1 == null) ? 0 : s1.hashCode());
    result = prime * result + ((s2 == null) ? 0 : s2.hashCode());
    hashCode = result;
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Transition other = (Transition) obj;
    if (l1 == null) {
      if (other.l1 != null) return false;
    } else if (!l1.equals(other.l1)) return false;
    if (s1 == null) {
      if (other.s1 != null) return false;
    } else if (!s1.equals(other.s1)) return false;
    if (s2 == null) {
      if (other.s2 != null) return false;
    } else if (!s2.equals(other.s2)) return false;
    return true;
  }

  @Override
  public String toString() {
    return s1 + "~" + l1 + "~>" + s2;
  }

  @Override
  public N getLabel() {
    return l1;
  }
}
