package test.cases.statics;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class SimpleSingleton extends AbstractBoomerangTest {
	@Test
	public void singletonDirect(){
		Alloc singleton = alloc;
		queryForAndNotEmpty(singleton);
	}
	private static Alloc alloc = new Alloc();
	@Test
	public void simpleWithAssign(){
		alloc = new Alloc();
	    Object b = alloc;
		queryFor(b);
	}
	@Test
	public void simpleWithAssign2(){
		alloc = new Alloc();
	    Object b = alloc;
	    Object a = alloc;
		queryFor(b);
	}
}
