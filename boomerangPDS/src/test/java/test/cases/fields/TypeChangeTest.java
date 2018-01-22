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
package test.cases.fields;

import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class TypeChangeTest extends AbstractBoomerangTest {
	@Test
	public void returnValue() {
		D f = new D();
		Object amIThere = f.getField();
		System.err.println(2);
		queryFor(amIThere);
	}
	@Test
	public void doubleReturnValue() {
		D f = new D();
		Object t = f.getDoubleField();
		queryFor(t);
	}
	@Test
	public void returnValueAndBackCast() {
		D f = new D();
		Object t = f.getField();
		AllocatedObject u = (AllocatedObject) t;
		queryFor(u);
	}
	public static class D {
		Alloc f = new Alloc();
		D d = new D();

		public Object getField() {
			Alloc varShouldBeThere = this.f;
			return varShouldBeThere;
		}

		public Object getDoubleField() {
			return d.getField();
		}
	}
}
