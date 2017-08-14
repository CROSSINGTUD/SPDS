package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class ReadTwiceSameFieldTest extends AbstractBoomerangTest {
	@Test
	public void recursiveTest() {
		Container a = new Container();
		Container c = a.d;
		Container alias = c.d;
		queryFor(alias);
	}

	@Test
	public void readFieldTwice() {
		Container a = new Container();
		Container c = a.d;
		Container alias = c.d;
		queryFor(alias);
	}

	private class Container {
		Container d;

		Container() {
			if (staticallyUnknown())
				d = new Alloc();
			else
				d = null;
		}

	}

	private class DeterministicContainer {
		DeterministicContainer d;

		DeterministicContainer() {
			d = new DeterministicAlloc();
		}

	}
	private class DeterministicAlloc extends DeterministicContainer implements AllocatedObject {

	}

	private class Alloc extends Container implements AllocatedObject {

	}

}
