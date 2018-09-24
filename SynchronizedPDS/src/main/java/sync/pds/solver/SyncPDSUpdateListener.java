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

import sync.pds.solver.nodes.Node;
import wpds.interfaces.Location;

public interface SyncPDSUpdateListener<Stmt extends Location, Fact>  {

	public void onReachableNodeAdded(Node<Stmt,Fact> reachableNode);

}
