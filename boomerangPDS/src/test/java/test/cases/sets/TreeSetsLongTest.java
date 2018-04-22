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
package test.cases.sets;


import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;


public class TreeSetsLongTest extends AbstractBoomerangTest{
	@Test
	public void addAndRetrieve(){
		Set<Object> set = new TreeSet<Object>();
		Alloc alias = new Alloc();
		set.add(alias);
//		alias = new Alloc();
//		set.add(alias);
//		alias = new Alloc();
//		set.add(alias);
//		alias = new Alloc();
//		set.add(alias);
//		alias = new Alloc();
//		set.add(alias);
		
		Object alias2 = null;
		for(Object o : set)
			alias2 = o;
		Object ir = alias2;
		Object query2 = ir;
		queryFor(query2);
	}
	
	@Override
	protected boolean includeJDK() {
		return true;
	}
}
