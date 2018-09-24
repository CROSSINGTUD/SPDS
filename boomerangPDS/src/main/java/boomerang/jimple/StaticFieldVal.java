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


	private StaticFieldVal(Value v, SootField field, SootMethod m, Statement unbalanced) {
		super(v, m, unbalanced);
		this.field = field;
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m == null) ? 0 : m.hashCode());
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		return result;
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
		if (m == null) {
			if (other.m != null)
				return false;
		} else if (!m.equals(other.m))
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
	
	@Override
	public Val asUnbalanced(Statement stmt) {
		return new StaticFieldVal(this.value(), field, m, stmt);
	}
}
