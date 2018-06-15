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

import soot.Local;
import soot.SootMethod;
import soot.Value;

public class Val {
	protected final SootMethod m;
	private final Value v;
	private final String rep; 

	private static Val zeroInstance;
	
	public Val(Value v, SootMethod m){
		if(v == null)
			throw new RuntimeException("Value must not be null!");
		this.v = v;
		this.m = m;
		this.rep = null;
		if(!isStatic()){
			if(!m.hasActiveBody())
				throw new RuntimeException("No active body for method");
			if(v instanceof Local && !m.getActiveBody().getLocals().contains(v)){
				throw new RuntimeException("Creating a Local with wrong method. " +v + " "+  m);
			}
		}
	}
	
	private Val(String rep){
		this.rep = rep;
		this.m = null;
		this.v = null;
	}

	public Value value(){
		return v;
	}
	
	public SootMethod m(){
		return m;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
//		result = prime * result + ((m == null) ? 0 : m.hashCode());
		result = prime * result + ((rep == null) ? 0 : rep.hashCode());
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		//Removed this as AllocVal.equals(Val)
//		if (getClass() != obj.getClass())
//			return false;
		Val other = (Val) obj;
		if (rep == null) {
			if (other.rep != null)
				return false;
		} else if (!rep.equals(other.rep))
			return false;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if(rep != null)
			return rep;
		return v.toString()+ " (" + m.getDeclaringClass().getShortName() +"." + m.getName() +")";
	}

	public static Val zero() {
		if(zeroInstance == null)
			zeroInstance = new Val("ZERO");
		return zeroInstance;
	}

	public boolean isStatic(){
		return false; 
	}

	public boolean isNewExpr(){
		return false;
	}

}
