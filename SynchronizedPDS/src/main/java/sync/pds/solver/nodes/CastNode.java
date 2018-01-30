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
package sync.pds.solver.nodes;

public class CastNode<Statement,Val, Type> extends Node<Statement,Val> {
	private final Type type;

	public CastNode(Statement stmt, Val variable, Type type) {
		super(stmt, variable);
		this.type = type;
	}
	
	public Type getType(){
		return type;
	}
}
