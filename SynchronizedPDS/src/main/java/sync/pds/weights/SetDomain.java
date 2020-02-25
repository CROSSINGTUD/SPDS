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
package sync.pds.weights;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;
import wpds.interfaces.Location;

public class SetDomain<N, Stmt, Fact> extends Weight {

  private static SetDomain one;
  private static SetDomain zero;
  private final String rep;
  private Collection<Node<Stmt, Fact>> nodes;

  private SetDomain(String rep) {
    this.rep = rep;
  }

  private SetDomain(Collection<Node<Stmt, Fact>> nodes) {
    this.nodes = nodes;
    this.rep = null;
  }

  public SetDomain(Node<Stmt, Fact> node) {
    this.nodes = Sets.<Node<Stmt, Fact>>newHashSet(node);
    this.rep = null;
  }

  @Override
  public Weight extendWith(Weight other) {
    if (other.equals(one())) {
      return this;
    }
    if (this.equals(one())) {
      return other;
    }
    return zero();
  }

  @Override
  public Weight combineWith(Weight other) {
    if (other.equals(zero())) return this;
    if (this.equals(zero())) return other;
    if (this.equals(one()) || other.equals(one())) return one();
    if (other instanceof SetDomain) {
      Set<Node<Stmt, Fact>> merged = Sets.newHashSet(nodes);
      merged.addAll(((SetDomain) other).nodes);
      return new SetDomain<N, Stmt, Fact>(merged);
    }
    return zero();
  }

  public static <N extends Location, Stmt, Fact> SetDomain<N, Stmt, Fact> one() {
    if (one == null) one = new SetDomain("<1>");
    return one;
  }

  public static <N extends Location, Stmt, Fact> SetDomain<N, Stmt, Fact> zero() {
    if (zero == null) zero = new SetDomain("<0>");
    return zero;
  }

  @Override
  public String toString() {
    if (rep != null) return rep;
    return nodes.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SetDomain other = (SetDomain) obj;
    if (nodes == null) {
      if (other.nodes != null) return false;
    } else if (!nodes.equals(other.nodes)) return false;
    if (rep == null) {
      if (other.rep != null) return false;
    } else if (!rep.equals(other.rep)) return false;
    return true;
  }

  public Collection<Node<Stmt, Fact>> elements() {
    return Sets.newHashSet(nodes);
  }
}
