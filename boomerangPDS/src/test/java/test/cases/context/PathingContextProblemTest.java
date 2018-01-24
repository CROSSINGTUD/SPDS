package test.cases.context;

import org.junit.Test;

import test.cases.basic.Allocation;
import test.core.AbstractBoomerangTest;

public class PathingContextProblemTest extends AbstractBoomerangTest {
	@Test
	public void start() {
		Inner i = new Inner();
		i.test1();
		i.test2();
	}

	private static class Inner {

		public void callee(Object a, Object b) {
			queryFor(a);
		}

		public void test1() {
			Object a1 = new Allocation();
			Object b1 = a1;
			callee(a1, b1);
		}

		public void test2() {
			Object a2 = new Object();
			Object b2 = new Object();
			callee(a2, b2);
		}
	}

}
