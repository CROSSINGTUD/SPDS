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
import wpds.interfaces.Location;

public class WitnessNode<Stmt extends Location, Fact, Field extends Location> {
	private Stmt stmt;
	private Fact fact;
	
	public WitnessNode(Stmt stmt, Fact fact){
		this.stmt = stmt;
		this.fact = fact;
	}
	
	
	public Node<Stmt,Fact> asNode(){
		return new Node<Stmt,Fact>(stmt,fact);
	}
	
	public Stmt stmt(){
		return stmt;
	}

	public Fact fact(){
		return fact;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fact == null) ? 0 : fact.hashCode());
		result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WitnessNode other = (WitnessNode) obj;
		if (fact == null) {
			if (other.fact != null)
				return false;
		} else if (!fact.equals(other.fact))
			return false;
		if (stmt == null) {
			if (other.stmt != null)
				return false;
		} else if (!stmt.equals(other.stmt))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Witness " + asNode();
	}
}
