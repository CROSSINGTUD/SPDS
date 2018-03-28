/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package boomerang.debugger;

import java.util.Map;
import java.util.Set;

import boomerang.Query;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import heros.utilities.DefaultValueMap;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.Weight;

public class Debugger<W extends Weight> {

	public void done(Map<Query, AbstractBoomerangSolver<W>> queryToSolvers) {
		// TODO Auto-generated method stub
		
	}

}
