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
package test.cases.synchronizd;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class BlockTest extends AbstractBoomerangTest {
	
	private Object field;

	@Test
	public void block(){
		synchronized (field) {
			AllocatedObject o = new Alloc();
			queryFor(o);
		}
	}
	@Test
	public void block2(){
		set();
		synchronized (field) {
			Object o = field;
			queryFor(o);
		}
	}
	private void set() {
		synchronized (field) {
			field = new Alloc();
		}
	}
}
