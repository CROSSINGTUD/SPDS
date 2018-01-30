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
package sync.pds.solver;

import wpds.interfaces.Location;

public abstract class SyncStatePDSUpdateListener<Stmt extends Location, Fact, Field extends Location>  {
	
	private WitnessNode<Stmt, Fact, Field> node;
	public SyncStatePDSUpdateListener(WitnessNode<Stmt,Fact, Field> node){
		this.node = node;
	}
	public abstract void reachable();
	public WitnessNode<Stmt, Fact, Field> getNode(){
		return node;
	}

}
