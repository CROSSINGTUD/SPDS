package test.cases.fields;

import org.junit.Test;
import test.core.AbstractBoomerangTest;

public class ObjectSensitivity extends AbstractBoomerangTest{

	@Test
	public void objectSensitivity(){
	    B b1 = new B();
	    Alloc b2 = new Alloc();

	    A a1 = new A(b1);
	    A a2 = new A(b2);

	    Object b3 = a1.getF();
	    Object b4 = a2.getF();

	    queryFor(b4);
	}
	
	private static class A{

		private Object f;

		public A(Object o) {
			this.f = o;
		}

		public Object getF() {
			return this.f;
		}
	}
}
