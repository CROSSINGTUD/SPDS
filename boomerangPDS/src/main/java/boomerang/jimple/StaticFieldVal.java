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
package boomerang.jimple;

import soot.SootField;
import soot.SootMethod;
import soot.Value;

public class StaticFieldVal extends Val {

	private final SootField field;

	public StaticFieldVal(Value v, SootField field, SootMethod m) {
		super(v, m);
		this.field = field;
	}

	@Override
	public int hashCode() {
		return field.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		StaticFieldVal other = (StaticFieldVal) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		return true;
	}

	@Override
	public boolean isStatic() {
		return true;
	} 
	public String toString() {
		return "StaticField: " + field;
	}

	public SootField field() {
		return field;
	};
}
