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
		Object alias = a.f();
		queryFor(alias);
	}
	private static class A{
		Object f;
		public void setF() {
			f = new AllocatedObject() {
			}; 
		}

		public Object f() {
			return f;
		}
		
	}
}
