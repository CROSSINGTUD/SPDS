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

public class ReturnPOITest extends AbstractBoomerangTest {
	private class A{
		B b;
	}
	private class B {
		C c;
	}
	private class C implements AllocatedObject{
	}
	
	
	@Test
	public void indirectAllocationSite(){
		B a = new B();
		B e = a;
		allocation(a);
		C alias = e.c;
		C query = a.c;
		queryFor(query);
	}

	private void allocation(B a) {
		C d = new C();
		a.c = d;
	}
	
	@Test
	public void unbalancedReturnPOI1(){
		C a = new C();
		B b =  new B();
		B c = b;
		setField(b,a);
		C alias = c.c;
		queryFor(a);
	}

	private void setField(B a2, C a) {
		a2.c = a;
	}

	@Test
	public void unbalancedReturnPOI3(){
		B b =  new B();
		B c = b;
		setField(c);
		C query = c.c;
		queryFor(query);
	}
	private void setField(B c) {
		c.c = new C();
	}

	@Test
	public void whyRecursiveReturnPOIIsNecessary(){
		C c = new C();
		B b =  new B();
		A a = new A();
		A a2 = a;
		a2.b = b;
		B b2 = b;
		setFieldTwo(a,c);
		C alias = a2.b.c;
		queryFor(c);
	}
	@Test
	public void whysRecursiveReturnPOIIsNecessary(){
		C c = new C();
		B b =  new B();
		A a = new A();
		A a2 = a;
		a2.b = b;
		B b2 = b;
		setFieldTwo(a,c);
		C alias = a2.b.c;
		queryFor(alias);
	}
	private void setFieldTwo(A b, C a) {
		b.b.c = a;
	}
}
