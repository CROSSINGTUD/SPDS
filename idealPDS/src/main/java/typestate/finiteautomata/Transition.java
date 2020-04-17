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
package typestate.finiteautomata;

public class Transition implements ITransition {
  private final State from;
  private final State to;
  private final String rep;

  public Transition(State from, State to) {
    this.from = from;
    this.to = to;
    this.rep = null;
  }

  private Transition(String rep) {
    this.from = null;
    this.to = null;
    this.rep = rep;
  }

  public State from() {
    return from;
  }

  public State to() {
    return to;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((from == null) ? 0 : from.hashCode());
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    result = prime * result + ((to == null) ? 0 : to.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Transition other = (Transition) obj;
    if (from == null) {
      if (other.from != null) return false;
    } else if (!from.equals(other.from)) return false;
    if (rep == null) {
      if (other.rep != null) return false;
    } else if (!rep.equals(other.rep)) return false;
    if (to == null) {
      if (other.to != null) return false;
    } else if (!to.equals(other.to)) return false;
    return true;
  }

  public String toString() {
    if (rep != null) return rep;
    return "" + from + " -> " + to;
  }

  private static Transition instance;

  public static Transition identity() {
    if (instance == null) instance = new Transition("ID -> ID");
    return instance;
  }
}
