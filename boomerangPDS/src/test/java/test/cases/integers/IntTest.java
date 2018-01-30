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
package test.cases.integers;

import org.junit.Test;

import test.core.AbstractBoomerangTest;

public class IntTest extends AbstractBoomerangTest {
	@Test
	public void simpleAssign(){
		int allocation = 1;
		intQueryFor(allocation);
	}
	@Test
	public void simpleIntraAssign(){
		int allocation = 1;
		int y = allocation;
		intQueryFor(y);
	}

	@Test
	public void simpleInterAssign(){
		int allocation = 1;
		int y = foo(allocation);
		intQueryFor(y);
	}
	@Test
	public void returnDirect(){
		int allocation = getVal();
		intQueryFor(allocation);
	}

	@Test
	public void returnInDirect(){
		int x = getValIndirect();
		intQueryFor(x);
	}
	private int getValIndirect() {
		int allocation = 1;
		return allocation;
	}
	private int getVal() {
		return 1;
	}
	private int foo(int x) {
		int y = x;
		return y;
	}
}
