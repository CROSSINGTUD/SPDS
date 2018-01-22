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
