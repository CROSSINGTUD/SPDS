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

public class SubclassingTest extends AbstractBoomerangTest{
	private static class Superclass{
		AllocatedObject o = new AllocatedObject(){};
	}
	
	private static class Subclass extends Superclass{
		
	}
	
	private static class ClassWithSubclassField{
		Subclass f;
		public ClassWithSubclassField(Subclass t){
			this.f = t;
		}
	}
	
	@Test
	public void typingIssue(){
		Subclass subclass = new Subclass();
		ClassWithSubclassField classWithSubclassField = new ClassWithSubclassField(subclass);
		AllocatedObject query = classWithSubclassField.f.o;
		queryFor(query);
	}
	
	
	
}
