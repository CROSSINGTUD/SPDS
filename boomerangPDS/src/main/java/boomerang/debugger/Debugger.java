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
package boomerang.debugger;

import java.util.Map;
import java.util.Set;

import boomerang.Query;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.Weight;

public class Debugger<W extends Weight> {

	public void reachableNodes(Query q, Map<Transition<Statement, INode<Val>>, W> map) {
	}

	public void callRules(Query q, Set<Rule<Statement, INode<Val>, W>> allRules) {
		
	}

	public void done() {
		// TODO Auto-generated method stub
		
	}

}
