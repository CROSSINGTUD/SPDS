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

import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;
import wpds.interfaces.Location;

public interface WeightFunctions<Stmt,Fact,Field, W extends Weight> {
	public W push(Node<Stmt,Fact> curr, Node<Stmt,Fact> succ, Field field);
	public W normal(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ);
	public W pop(Node<Stmt, Fact> curr, Field location);
	public W getOne();
	public W getZero();
}
