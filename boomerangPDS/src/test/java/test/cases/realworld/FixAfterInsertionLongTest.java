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
package test.cases.realworld;

import org.junit.Ignore;
import org.junit.Test;

import test.cases.realworld.FixAfterInsertion.Entry;
import test.core.AbstractBoomerangTest;

@Ignore
public class FixAfterInsertionLongTest  extends AbstractBoomerangTest{

	@Test
	public void main(){
		Entry<Object, Object> entry = new Entry<Object,Object>(null,null,null);
		entry = new Entry<Object,Object>(null,null,entry);
		new FixAfterInsertion<>().fixAfterInsertion(entry);
		Entry<Object, Object> query = entry.parent;
		queryFor(query);
	}

	@Test
	public void rotateLeftAndRightInLoop(){
		Entry<Object, Object> entry = new Entry<Object,Object>(null,null,null);
		entry = new Entry<Object,Object>(null,null,entry);
		while(true){
			new FixAfterInsertion<>().rotateLeft(entry);	
			new FixAfterInsertion<>().rotateRight(entry);
			if(staticallyUnknown())
				break;
		}
		Entry<Object, Object> query = entry.parent;
		queryFor(query);
	}
}
