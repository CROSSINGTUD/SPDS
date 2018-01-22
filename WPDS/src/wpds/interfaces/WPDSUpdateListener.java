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
package wpds.interfaces;

import wpds.impl.Rule;
import wpds.impl.Weight;

public interface WPDSUpdateListener<N extends Location, D extends State, W extends Weight> {

	public void onRuleAdded(Rule<N, D, W> rule);

}
