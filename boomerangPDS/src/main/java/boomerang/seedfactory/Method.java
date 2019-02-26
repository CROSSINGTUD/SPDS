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
package boomerang.seedfactory;

import soot.SootMethod;
import wpds.interfaces.Location;

/**
 * Created by johannesspath on 07.12.17.
 */
public class Method implements Location {

    private static Method epsilon;
    private final SootMethod delegate;

    public Method(SootMethod m){
        this.delegate = m;
    }

    private Method(){
        this.delegate = null;
    }

    public static Method epsilon(){
        if(epsilon == null)
            epsilon = new Method() {
        	@Override
        	public int hashCode() {
        		return System.identityHashCode(this);
        	}
        	@Override
    		public boolean equals(Object obj) {
    			return obj == this;
    		}
        };
        return epsilon;
    }
   

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
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
		Method other = (Method) obj;
		if (delegate == null) {
			if (other.delegate != null)
				return false;
		} else if (!delegate.equals(other.delegate))
			return false;
		return true;
	}

	public SootMethod getMethod() {
        return delegate;
    }
    @Override
    public String toString() {
    	return delegate != null ? delegate.toString() : "METHOD EPS";
    }
}
