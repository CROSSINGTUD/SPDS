package test.cases.exceptions;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

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
