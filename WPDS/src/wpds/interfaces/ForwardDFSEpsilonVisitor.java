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
package wpds.interfaces;

import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class ForwardDFSEpsilonVisitor<N extends Location,D extends State, W extends Weight> extends ForwardDFSVisitor<N,D,W>{


	public ForwardDFSEpsilonVisitor(WeightedPAutomaton<N, D, W> aut) {
		super(aut);
	}

	@Override
	protected boolean continueWith(Transition<N, D> t) {
		return t.getLabel() instanceof Empty;
	}
}
