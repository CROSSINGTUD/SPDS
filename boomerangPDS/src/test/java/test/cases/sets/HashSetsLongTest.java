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
package test.cases.sets;

import java.util.HashSet;

import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;


public class HashSetsLongTest extends AbstractBoomerangTest{
	@Test
	public void addAndRetrieve(){
		HashSet<Object> set = new HashSet<>();
		AllocatedObject alias = new AllocatedObject(){};
		AllocatedObject alias3 = new AllocatedObject(){};
		set.add(alias);
		set.add(alias3);
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
