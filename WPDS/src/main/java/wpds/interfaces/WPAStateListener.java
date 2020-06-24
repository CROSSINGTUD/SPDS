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
package wpds.interfaces;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public abstract class WPAStateListener<N extends Location, D extends State, W extends Weight> {

  protected final D state;

  public WPAStateListener(D state) {
    this.state = state;
  }

  public abstract void onOutTransitionAdded(
      Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> weightedPAutomaton);

  public abstract void onInTransitionAdded(
      Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> weightedPAutomaton);

  public D getState() {
    return state;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    WPAStateListener other = (WPAStateListener) obj;
    if (state == null) {
      if (other.state != null) return false;
    } else if (!state.equals(other.state)) return false;
    return true;
  }
}
