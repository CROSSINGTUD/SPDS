package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class TypeChangeTest extends AbstractBoomerangTest {
	@Test
	public void returnValue() {
		D f = new D();
		Object t = f.getField();
		queryFor(t);
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
	public class D {
		AllocatedObject f = new AllocatedObject(){};
		D d = new D();

		public Object getField() {
			return f;
		}

		public Object getDoubleField() {
			return d.getField();
		}
	}
}
