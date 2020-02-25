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
package sync.pds.solver;

import wpds.impl.NormalRule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public class CastNormalRule<N extends Location, D extends State, W extends Weight>
    extends NormalRule<N, D, W> {

  public CastNormalRule(D s1, N l1, D s2, N l2, W w) {
    super(s1, l1, s2, l2, w);
  }

  @Override
  public boolean canBeApplied(Transition<N, D> t, W weight) {
    return super.canBeApplied(t, weight);
  }
}
