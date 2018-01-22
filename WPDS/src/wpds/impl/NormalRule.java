/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package wpds.impl;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public class NormalRule<N extends Location, D extends State, W extends Weight> extends
    Rule<N, D, W> {

  public NormalRule(D s1, N l1, D s2, N l2, W w) {
    super(s1, l1, s2, l2, w);
  }

  @Override
  public String toString() {
    return "<" + s1 + ";" + l1 + ">-><" + s2 + ";" + l2 + ">"
        + ((w instanceof Weight.NoWeight) ? "" : "(" + w + ")");
  }

	public boolean canBeApplied(Transition<N, D> t, W weight) {
		return true;
	}
}
