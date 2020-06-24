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

import wpds.interfaces.Location;
import wpds.interfaces.State;

public abstract class Rule<N extends Location, D extends State, W extends Weight> {
  protected N l1;
  protected D s1;
  protected N l2;
  protected D s2;
  protected W w;

  public Rule(D s1, N l1, D s2, N l2, W w) {
    this.l1 = l1;
    this.s1 = s1;
    this.l2 = l2;
    this.s2 = s2;
    this.w = w;
  }

  public Configuration<N, D> getStartConfig() {
    return new Configuration<N, D>(l1, s1);
  }

  public Configuration<N, D> getTargetConfig() {
    return new Configuration<N, D>(l2, s2);
  }

  public N getL1() {
    return l1;
  }

  public N getL2() {
    return l2;
  }

  public D getS1() {
    return s1;
  }

  public D getS2() {
    return s2;
  }

  public void setS1(D s1) {
    this.s1 = s1;
  }

  public W getWeight() {
    return w;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((l1 == null) ? 0 : l1.hashCode());
    result = prime * result + ((l2 == null) ? 0 : l2.hashCode());
    result = prime * result + ((s1 == null) ? 0 : s1.hashCode());
    result = prime * result + ((s2 == null) ? 0 : s2.hashCode());
    result = prime * result + ((w == null) ? 0 : w.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Rule other = (Rule) obj;
    if (l1 == null) {
      if (other.l1 != null) return false;
    } else if (!l1.equals(other.l1)) return false;
    if (l2 == null) {
      if (other.l2 != null) return false;
    } else if (!l2.equals(other.l2)) return false;
    if (s1 == null) {
      if (other.s1 != null) return false;
    } else if (!s1.equals(other.s1)) return false;
    if (s2 == null) {
      if (other.s2 != null) return false;
    } else if (!s2.equals(other.s2)) return false;
    if (w == null) {
      if (other.w != null) return false;
    } else if (!w.equals(other.w)) return false;
    return true;
  }
}
