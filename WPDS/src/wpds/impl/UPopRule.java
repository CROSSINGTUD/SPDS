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

import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public class UPopRule<N extends Location, D extends State> extends PopRule<N, D, NoWeight> {

  public UPopRule(D s1, N l1, D s2) {
    super(s1, l1, s2, NoWeight.NO_WEIGHT_ONE);
  }


  @Override
  public String toString() {
    return "<" + s1 + ";" + l1 + ">-><" + s2 + ">";
  }

}
