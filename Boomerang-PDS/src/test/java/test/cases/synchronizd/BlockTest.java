package test.cases.synchronizd;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class BlockTest extends AbstractBoomerangTest {
	
	private Object field;

	@Test
	public void block(){
		synchronized (field) {
			AllocatedObject o = new AllocatedObject(){};
			queryFor(o);
		}
	}
}
