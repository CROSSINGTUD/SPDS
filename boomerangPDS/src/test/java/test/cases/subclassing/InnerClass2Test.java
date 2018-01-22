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
package test.cases.subclassing;

import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class InnerClass2Test extends AbstractBoomerangTest {
	public void doThings(final Object name) {
		class MyInner {
			public void seeOuter() {
				queryFor(name);
			}
		}
		MyInner inner = new MyInner();
		inner.seeOuter();
	}
	@Test
	public void run(){
		Object alloc = new Allocation();
		String cmd = System.getProperty("");
		if(cmd!=null){
			alloc = new Allocation();
		}
		InnerClass2Test outer = new InnerClass2Test();
		outer.doThings(alloc);
	}
	private class Allocation implements AllocatedObject{
		
	}
}
