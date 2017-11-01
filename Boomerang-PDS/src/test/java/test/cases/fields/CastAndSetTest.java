package test.cases.fields;

import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class CastAndSetTest extends AbstractBoomerangTest{
	@Test
	public void setAndGet(){
		Container container = new Container();
		Object o1 = new Object();
		container.set(o1);
		AllocatedObject o2 = new Alloc();
		container.set(o2);
		AllocatedObject alias = container.get();
		queryFor(alias);
	}
	private static class Container{
		AllocatedObject o;
		public void set(Object o1) {
			AllocatedObject var = (AllocatedObject) o1;
			this.o = var;
		}

		public AllocatedObject get() {
			return o;
		}
	}
}
