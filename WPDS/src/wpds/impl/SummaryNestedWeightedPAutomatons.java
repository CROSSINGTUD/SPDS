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

import java.util.Map;

import com.google.common.collect.Maps;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public class SummaryNestedWeightedPAutomatons<N extends Location, D extends State, W extends Weight> implements NestedWeightedPAutomatons<N, D, W> {

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
