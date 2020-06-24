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

public interface NestedWeightedPAutomatons<N extends Location, D extends State, W extends Weight> {

  void putSummaryAutomaton(D target, WeightedPAutomaton<N, D, W> aut);

  WeightedPAutomaton<N, D, W> getSummaryAutomaton(D target);
}
