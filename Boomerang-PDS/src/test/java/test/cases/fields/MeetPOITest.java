package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class MeetPOITest extends AbstractBoomerangTest {

	@Test
	public void wrappedAlloc() {
		A e = new A();
		A g = e;
		wrapper(g);
		C h = e.b.c;
		queryFor(h);
	}

	private void wrapper(A g) {
		alloc(g);
	}

	private void alloc(A g) {
		g.b.c = new C();
	}

	public class A {
		B b = new B();
	}

	public class B {
		C c;
	}

	public class C implements AllocatedObject {
		String g;
	}
}
