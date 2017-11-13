package test.cases.synchronizd;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class BlockTest extends AbstractBoomerangTest {
	
	private Object field;

	@Test
	public void block(){
		synchronized (field) {
			AllocatedObject o = new Alloc();
			queryFor(o);
		}
	}
	@Test
	public void block2(){
		set();
		synchronized (field) {
			Object o = field;
			queryFor(o);
		}
	}
	private void set() {
		synchronized (field) {
			field = new Alloc();
		}
	}
}
