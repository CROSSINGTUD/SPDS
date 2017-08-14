package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class ThreeFieldsTest extends AbstractBoomerangTest{
	public class Level1 {
		Level2 l2 = new Level2();
	}

	public class Level2 {
		Level3 l3 = new Level3();
	}

	public class Level3 {
		Level4 l4;
	}

	public class Level4 implements AllocatedObject {

	}

	@Test
	public void indirectAllocationSite() {
		Level1 l = new Level1();
		Level2 x = l.l2;
		setField(l);
		Level4 alias = l.l2.l3.l4;
		Level4 alias2 = x.l3.l4;
		queryFor(alias2);
	}

	public void setField(Level1 l) {
		l.l2.l3.l4 = new Level4();
	}
	
	@Test
	public void test() {
		Level1 l = new Level1();
		Level2 x = l.l2;
		wrappedSetField(l);
		Level4 alias = l.l2.l3.l4;
		Level4 alias2 = x.l3.l4;
		queryFor(alias2);
	}

	private void wrappedSetField(Level1 l) {
		setField(l);
	}

}
