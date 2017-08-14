package test.cases.context;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class SimpleContextQueryTest extends AbstractBoomerangTest {
	@Test
	public void outerAllocation(){
		AllocatedObject alloc = new AllocatedObject(){};
		methodOfQuery(alloc);
	}

	private void methodOfQuery(AllocatedObject alloc) {
		AllocatedObject alias = alloc;
		queryFor(alias);
	}
	@Test
	public void outerAllocation2(){
		AllocatedObject alloc = new AllocatedObject(){};
		AllocatedObject same = alloc;
		methodOfQuery(alloc, same);
	}
	@Test
	public void outerAllocation3(){
		AllocatedObject alloc = new AllocatedObject(){};
		Object same = new Object();
		methodOfQuery(alloc, same);
	}


	private void methodOfQuery(Object alloc, Object alias) {
		queryFor(alloc);
	}
}
