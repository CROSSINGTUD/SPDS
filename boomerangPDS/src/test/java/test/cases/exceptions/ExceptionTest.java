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
package test.cases.exceptions;

import org.junit.Ignore;
import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

@Ignore
public class ExceptionTest extends AbstractBoomerangTest{
	@Test
	public void compiletimeExceptionFlow(){
		try{
			throwException();
		} catch(MyException e){
			Alloc object = e.field;
			queryFor(e);	
		}
	}
	@Test
	public void runtimeExceptionFlow(){
		try{
			throwRuntimeException();
		} catch(MyRuntimeException e){
			Alloc object = e.field;
			queryFor(e);	
		}
	}

	private void throwRuntimeException() {
		new MyRuntimeException(new Alloc());
	}

	private static class MyRuntimeException extends RuntimeException{
		Alloc field;
		public MyRuntimeException(Alloc alloc) {
			field = alloc;
		}
	}
	private void throwException() throws MyException {
		throw new MyException(new Alloc());
	}
	private static class MyException extends Exception{
		Alloc field;
		public MyException(Alloc alloc) {
			field = alloc;
		}
	}
}
