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
package boomerang;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.Type;
import sync.pds.solver.nodes.Node;

public abstract class Query{

	private final Statement stmt;
	private final Val variable;
	private final List<Field> fields;

	public Query(Statement stmt, Val variable) {
		this.stmt = stmt;
		this.variable = variable;
		this.fields = Lists.newArrayList();
		this.fields.add(Field.empty());
	}

	public Query(Statement stmt, Val variable, List<Field> fields) {
		this.stmt = stmt;
		this.variable = variable;
		this.fields = fields;
	}
	
	public Node<Statement,Val> asNode(){
		return new Node<Statement,Val>(stmt,variable);
	}
	@Override
	public String toString() {
		return new Node<Statement,Val>(stmt,variable).toString();
	}
	
	public Statement stmt(){
		return stmt;
	}
	
	public Val var(){
		return variable;
	}

	public List<Field> getFields() {
		return fields;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
		result = prime * result + ((variable == null) ? 0 : variable.hashCode());
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
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
		Query other = (Query) obj;
		if (stmt == null) {
			if (other.stmt != null)
				return false;
		} else if (!stmt.equals(other.stmt))
			return false;
		if (variable == null) {
			if (other.variable != null)
				return false;
		} else if (!variable.equals(other.variable))
			return false;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}

	public Type getType() {
		return variable.getType();
	}
}
