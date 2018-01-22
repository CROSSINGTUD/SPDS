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
package sync.pds.solver.nodes;

import sync.pds.solver.SyncPDSSolver.PDSSystem;

public class CallPopNode<Location, Stmt> extends PopNode<Location> {

	private final Stmt returnSite;
	public CallPopNode(Location location, PDSSystem system, Stmt returnSite) {
		super(location, system);
		this.returnSite = returnSite;
	}
	
	public Stmt getReturnSite(){
		return returnSite;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((returnSite == null) ? 0 : returnSite.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CallPopNode other = (CallPopNode) obj;
		if (returnSite == null) {
			if (other.returnSite != null)
				return false;
		} else if (!returnSite.equals(other.returnSite))
			return false;
		return true;
	}

}
