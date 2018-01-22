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
package test.cases.typing;

import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class TypeConfusion extends AbstractBoomerangTest{
	@Test
	public void invokesInterface(){
		B b = new B();
		A a1 = new A();
		Object o = b;
		A a = null;
		if(staticallyUnknown()){
			a = a1;
		} else{
			a = (A)o;
		} 
		queryFor(a);
	}
	

	
	private static class A implements AllocatedObject{
	}	
	private static class B {
	}
	
}
