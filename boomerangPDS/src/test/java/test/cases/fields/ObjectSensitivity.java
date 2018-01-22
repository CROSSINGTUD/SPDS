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

public class ObjectSensitivity extends AbstractBoomerangTest{

	@Test
	public void objectSensitivity1(){
	    B b1 = new B();
	    Alloc b2 = new Alloc();

	    A a1 = new A(b1);
	    A a2 = new A(b2);

	    Object b3 = a1.getF();
	    Object b4 = a2.getF();

	    queryFor(b4);
	}
	@Test
	public void objectSensitivity2(){
	    Alloc b2 = new Alloc();
	    A a2 = new A(b2);

	    otherScope();
	    Object b4 = a2.getF();

	    queryFor(b4);
	}
	private void otherScope() {
	    B b1 = new B();
	    A a1 = new A(b1);
	    Object b3 = a1.getF();
	}
	private static class A{

		private Object f;

		public A(Object o) {
			this.f = o;
		}

		public Object getF() {
			return this.f;
		}
	}
}
