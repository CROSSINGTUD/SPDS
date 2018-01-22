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
package test.cases.context;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class SimpleContextQueryTest extends AbstractBoomerangTest {
	@Test
	public void outerAllocation(){
		AllocatedObject alloc = new Alloc();
		methodOfQuery(alloc);
	}

	private void methodOfQuery(AllocatedObject alloc) {
		AllocatedObject alias = alloc;
		queryFor(alias);
	}
	@Test
	public void outerAllocation2(){
		AllocatedObject alloc = new AllocatedObject(){};
		AllocatedObject same = alloc;
		methodOfQuery(alloc, same);
	}
	@Test
	public void outerAllocation3(){
		AllocatedObject alloc = new AllocatedObject(){};
		Object same = new Object();
		methodOfQuery(alloc, same);
	}


	private void methodOfQuery(Object alloc, Object alias) {
		queryFor(alloc);
	}
}
