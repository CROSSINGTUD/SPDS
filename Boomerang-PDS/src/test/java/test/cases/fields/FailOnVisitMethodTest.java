package test.cases.fields;


import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;
import test.core.selfrunning.NullableField;

public class FailOnVisitMethodTest extends AbstractBoomerangTest {
	private class A{
		B b = new B();
		E e = new E();
	}
	private class B implements AllocatedObject{
	}
	private static class E{
		public void bar(){
			
		}
	}
	
	@Test
	public void failOnVisitBar(){
		A a = new A();
		B alias = a.b;
		E e = a.e;
		e.bar();
		queryFor(alias);
	}
	private class C{
		B b = null;
		E e = null;
	}
	@Test
	public void failOnVisitBarSameMethod(){
		C a = new C();
		a.b = new B();
		B alias = a.b;
		E e = a.e;
		e.bar();
		queryFor(alias);
	}

	@Test
	public void failOnVisitBarSameMethodAlloc(){
		C a = new C();
		a.b = new B();
		a.e = new E();
		B alias = a.b;
		E e = a.e;
		e.bar();
		queryFor(alias);
	}

	@Test
	public void failOnVisitBarSameMethodSimpleAlloc(){
		Simplified a = new Simplified();
		a.e = new E();
		N alias = a.b;
		E e = a.e;
		e.bar();
		queryFor(alias);
	}
	
	private class Simplified{
		E e = null;
		N b = null;
	}


	@Test
	public void doNotVisitBar(){
		O a = new O();
		N alias = a.nullableField;
		queryFor(alias);
	}
	
	private static class O{
		N nullableField = null;
		E e = null;
		private O(){
			e = new E();
			e.bar();
		}
	}
	private static class N implements NullableField{}
	@Override
	protected Collection<String> errorOnVisitMethod() {
		return Collections.singleton("<test.cases.fields.FailOnVisitMethodTest$E: void bar()>");
	}
}
