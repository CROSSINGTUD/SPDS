package test.cases.statics;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.selfrunning.AbstractBoomerangTest;

public class StaticFieldFlows extends AbstractBoomerangTest {
	private static Object alloc;
	@Test
	public void simple(){
		alloc = new Alloc();
		Object alias = alloc;
		queryFor(alias);
	}
	
	@Test
	public void overwriteStatic(){
		alloc = new Object();
		alloc = new Alloc();
		Object alias = alloc;
		queryFor(alias);
	}
	@Test
	public void intraprocedural(){
		setStaticField();
		Object alias = alloc;
		queryFor(alias);
	}
	private void setStaticField() {
		alloc = new Alloc();
	}
}
