package test.cases.statics;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class SimpleSingleton extends AbstractBoomerangTest {
	@Test
	public void singletonDirect(){
		Alloc singleton = alloc;
		queryFor(singleton);
	}
	private static Alloc alloc = new Alloc();
}
