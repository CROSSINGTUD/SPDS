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
package test.cases.sets;

public class MyMap {
	MyInnerMap m = new MyInnerMap();
	public void add(Object o){
		MyInnerMap map = this.m;
		map.innerAdd(o);
		MyInnerMap alias = this.m;
		Object retrieved = alias.content;
	}
	public Object get(){
		MyInnerMap map = this.m;
		return map.get();
	}
}
