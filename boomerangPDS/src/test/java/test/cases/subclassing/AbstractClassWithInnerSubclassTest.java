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

public class AbstractClassWithInnerSubclassTest extends AbstractBoomerangTest{
	private static class Superclass{
		Element e;
	}
	
	private static class Subclass extends Superclass{
		Subclass(){
			e = new InnerClass();
		}
		private class InnerClass implements Element{
			AnotherClass c = new AnotherClass();

			@Override
			public AnotherClass get() {
				return c;
			}
		}
	}
	
	private static class AnotherClass{
		AllocatedObject o = new AllocatedObject(){};
	}
	
	@Test
	public void typingIssue(){
		Subclass subclass2 = new Subclass();
		AllocatedObject query = subclass2.e.get().o;
		queryFor(query);
	}
	
	private static interface Element{
		AnotherClass get();
	}
	
}
