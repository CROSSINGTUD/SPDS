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
package boomerang;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;

public class ForwardQuery extends Query{

	public ForwardQuery(Statement stmt, Val variable) {
		super(stmt, variable);
	}
	@Override
	public String toString() {
		return "ForwardQuery: "+ super.toString();
	}
}
