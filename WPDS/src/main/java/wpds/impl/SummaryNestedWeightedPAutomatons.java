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

import com.google.common.collect.Maps;
import java.util.Map;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public class SummaryNestedWeightedPAutomatons<N extends Location, D extends State, W extends Weight>
    implements NestedWeightedPAutomatons<N, D, W> {

  private Map<D, WeightedPAutomaton<N, D, W>> summaries = Maps.newHashMap();

  @Override
  public void putSummaryAutomaton(D target, WeightedPAutomaton<N, D, W> aut) {
    summaries.put(target, aut);
  }

  @Override
  public WeightedPAutomaton<N, D, W> getSummaryAutomaton(D target) {
    return summaries.get(target);
  }
}
