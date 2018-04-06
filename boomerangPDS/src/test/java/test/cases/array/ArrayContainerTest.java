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
package test.cases.array;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;


public class ArrayContainerTest extends AbstractBoomerangTest {

	
	private static class ArrayContainer{
		AllocatedObject[] array = new AllocatedObject[]{};
		void put(Object o){
			array[0] = (AllocatedObject) o;
		}
		AllocatedObject get(){
			return array[0];
		}
	}

	@Test
	public void insertAndGet(){
		ArrayContainer container = new ArrayContainer();
		Object o1 = new Object();
		container.put(o1);
		AllocatedObject o2 = new Alloc();
		container.put(o2);
		AllocatedObject alias = container.get();
		queryFor(alias);
	}
	
	@Test
	public void insertAndGetField(){
		ArrayContainerWithPublicFields container = new ArrayContainerWithPublicFields();
		AllocatedObject o2 = new Alloc();
		container.array[0] = o2;
		AllocatedObject alias = container.array[0];
		queryFor(alias);
	}

	public static class ArrayContainerWithPublicFields{
		public AllocatedObject[] array = new AllocatedObject[]{};
	}

	@Test
	public void insertAndGet2(){
		ArrayContainer container = new ArrayContainer();
		Object o1 = new Object();
		container.put(o1);
		AllocatedObject o2 = new Alloc();
		container.put(o2);
		AllocatedObject alias = container.get();
		queryFor(alias);
	}
	
	@Test
	public void insertAndGetDouble(){
		ArrayOfArrayOfContainers outerContainer = new ArrayOfArrayOfContainers();
		ArrayContainer container = new ArrayContainer();
		Object o1 = new Object();
		container.put(o1);
		AllocatedObject o2 = new Alloc();
		container.put(o2);
		outerContainer.put(container);
		ArrayContainer aliasContainer = outerContainer.get();
		AllocatedObject alias = aliasContainer.get();
		queryFor(alias);
	}

	private static class ArrayOfArrayOfContainers{
		ArrayContainer[] array = new ArrayContainer[]{};
		void put(ArrayContainer o){
			array[0] = o;
		}
		ArrayContainer get(){
			return array[0];
		}
	}
}
