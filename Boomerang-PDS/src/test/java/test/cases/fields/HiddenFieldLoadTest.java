package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class HiddenFieldLoadTest extends AbstractBoomerangTest{
	@Test
	public void run(){
		A b = new A();
		A a = b;
		b.setF();
		int x = 1;
		Object alias = a.f();
		queryFor(alias);
	}
	@Test
	public void run1(){
		A b = new A();
		A a = b;
		b.setF();
		int x = 1;
		Object alias = a.f;
		queryFor(alias);
	}
	@Test
	public void run3(){
		A b = new A();
		A a = b;
		Alloc alloc = new Alloc();
		b.setF(alloc);
//		int x =1;
		Object alias = a.f();
		queryFor(alias);
	}
	@Test
	public void run2(){
		A b = new A();
		A a = b;
		b.f = new Alloc();
		Object alias = a.f();
		queryFor(alias);
	}
	
	@Test
	public void run4(){
		A b = new A();
		A a = b;
		b.f = new Alloc();
		Object alias = a.f;
		queryFor(alias);
	}
	private static class A{
		Object f;
		public void setF() {
			f = new Alloc();
		}

		public void setF(Object alloc) {
			f = alloc;
		}
		public Object f() {
			return f;
		}
		
	}
}
