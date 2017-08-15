package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class NoIndirectionTest extends AbstractBoomerangTest {
	private class A{
		Alloc b = null;
		Alloc c = null;
	}
	
	private class B{
		A a;
	}
	
	@Test
	public void doubleWriteAndReadFieldPositive(){
		Alloc query = new Alloc();
		A a = new A();
		B b = new B();
		a.b = query;
		b.a = a;
		A c = b.a;
		Alloc alias = c.b;
		reachable(alias);
	}
	
	@Test
	public void doubleWriteAndReadFieldNegative(){
		Alloc query = new Alloc();
		A a = new A();
		B b = new B();
		a.b = query;
		b.a = a;
		A c = b.a;
		Alloc alias = c.c;
		unreachable(alias);
	}

	@Test
	public void writeWithinCallPositive(){
		Alloc query = new Alloc();
		A a = new A();
		call(a, query);
		Alloc alias = a.b;
		reachable(alias);
	}
	@Test
	public void writeWithinCallNegative(){
		Alloc query = new Alloc();
		A a = new A();
		call(a, query);
		Alloc alias = a.c;
		unreachable(alias);
	}
	
	@Test
	public void writeWithinCallSummarizedPositive(){
		Alloc query = new Alloc();
		A a = new A();
		call(a, query);
		Alloc alias = a.b;
		A b = new A();
		call(b, alias);
		Alloc summarizedAlias = a.b;
		reachable(summarizedAlias);
	}
	
	private void call(A a, Alloc query) {
		a.b = query;
	}
	
	@Test
	public void doubleWriteWithinCallPositive(){
		Alloc query = new Alloc();
		A a = new A();
		B b = callAndReturn(a, query);
		A first = b.a;
		Alloc alias = first.b;
		reachable(alias);
	}


	private B callAndReturn(A a, Alloc query) {
		a.b = query;
		B b = new B();
		b.a = a;
		return b;
	}

	@Test
	public void overwriteFieldTest(){
		Alloc query = new Alloc();
		A a = new A();
		a.b = query;
		a.b = null;
		Alloc alias = a.b;
		unreachable(alias);
	}


	@Test
	public void overwriteButPositiveFieldTest(){
		Alloc query = new Alloc();
		A a = new A();
		a.b = query;
		a.c = null;
		Alloc alias = a.b;
		reachable(alias);
	}
	
	
	private class Alloc implements AllocatedObject{};
}