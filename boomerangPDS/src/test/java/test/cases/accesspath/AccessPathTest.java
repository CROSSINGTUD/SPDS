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
package test.cases.accesspath;

import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class AccessPathTest extends AbstractBoomerangTest {
	private static class A{
		B b = null;
	}
	private static class B implements AllocatedObject{
		B c = null;
		B d = null;
	}
	
	@Test
	public void getAllAliases(){
		A a = new A();
		B alloc = new B();
		a.b = alloc;
		queryFor(alloc);
	}
	@Test
	public void getAllAliasesBranched(){
		A a = new A();
		A b = new A();
		B alloc = new B();
		if(staticallyUnknown()){
			a.b = alloc;
		} else{
			b.b = alloc;
		}
		queryFor(alloc);
	}
	@Test
	public void getAllAliasesLooped(){
		A a = new A();
		B alloc = new B();
		a.b = alloc;
		for(int i = 0; i < 10;  i++){
			B d = alloc;
			alloc.c = d;
		}
		queryFor(alloc);
	}
	@Test
	public void getAllAliasesLoopedComplex(){
		A a = new A();
		B alloc = new B();
		a.b = alloc;
		for(int i = 0; i < 10;  i++){
			B d = alloc;
			if(staticallyUnknown())
				alloc.c = d;
			if(staticallyUnknown())
				alloc.d = d;
		}
		queryFor(alloc);
	}
}
