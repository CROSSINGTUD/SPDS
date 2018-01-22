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
package sync.pds.solver;

import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.interfaces.Location;

public interface WitnessListener<Stmt extends Location,Fact,Field extends Location> {

	void fieldWitness(Transition<Field, INode<Node<Stmt, Fact>>> transition);

	void callWitness(Transition<Stmt, INode<Fact>> t);

}
