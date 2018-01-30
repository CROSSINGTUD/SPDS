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

import wpds.interfaces.State;

/**
 * Created by johannesspath on 07.12.17.
 */
public class Reachable implements State {

    private static Reachable instance;
    private static Reachable entry;
    private Reachable(){}

    public static Reachable v(){
        if(instance == null)
            instance = new Reachable();
        return instance;
    }

    public static Reachable entry(){
        if(entry == null)
        	entry = new Reachable();
        return entry;
    }
    @Override
    public String toString() {
    	return "0";
    }
}
