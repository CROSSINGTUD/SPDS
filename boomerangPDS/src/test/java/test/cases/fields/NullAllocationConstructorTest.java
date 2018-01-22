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

public class NullAllocationConstructorTest extends AbstractBoomerangTest{
	private class A{
		B f = null;
	}
	private class B{
		
	}
	@Test
	public void nullAllocationOfField(){
		A a = new A();
		B variable = a.f;
		queryFor(variable);
	}
}
