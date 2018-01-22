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

import pathexpression.LabeledGraph;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public abstract class PAutomaton<N extends Location, D extends State> extends WeightedPAutomaton<N, D, NoWeight>
		implements LabeledGraph<D, N> {
	
	public PAutomaton(D initialState) {
		super(initialState);
	}

	@Override
	public NoWeight getOne() {
		return NoWeight.NO_WEIGHT_ONE;
	}

	@Override
	public NoWeight getZero() {
		return NoWeight.NO_WEIGHT_ZERO;
	}
}
