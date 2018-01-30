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
            epsilon = new Method();
        return epsilon;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Method method = (Method) o;

        return delegate != null ? delegate.equals(method.delegate) : method.delegate == null;
    }

    @Override
    public int hashCode() {
        return delegate != null ? delegate.hashCode() : 0;
    }

    public SootMethod getMethod() {
        return delegate;
    }
}
