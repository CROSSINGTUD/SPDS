package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class InterproceduralTest extends AbstractBoomerangTest {
	@Test
	public void test3() {
		A a = new A();
		B b = new B();
		b.c = new C();
		alias(a, b);
		B h = a.b;
		C query = h.c;
		queryFor(query);
	}

	private void alias(A a, B b) {
		a.b = b;
	}

	public class A {
		B b;
	}

	public class B {
		C c;
	}

	public class C implements AllocatedObject{
	}
}
