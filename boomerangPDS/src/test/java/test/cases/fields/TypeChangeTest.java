package test.cases.fields;

import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class TypeChangeTest extends AbstractBoomerangTest {
	@Test
	public void returnValue() {
		D f = new D();
		Object amIThere = f.getField();
		System.err.println(2);
		queryFor(amIThere);
	}
	@Test
	public void doubleReturnValue() {
		D f = new D();
		Object t = f.getDoubleField();
		queryFor(t);
	}
	@Test
	public void returnValueAndBackCast() {
		D f = new D();
		Object t = f.getField();
		AllocatedObject u = (AllocatedObject) t;
		queryFor(u);
	}
	public static class D {
		Alloc f = new Alloc();
		D d = new D();

		public Object getField() {
			Alloc varShouldBeThere = this.f;
			return varShouldBeThere;
		}

		public Object getDoubleField() {
			return d.getField();
		}
	}
}
