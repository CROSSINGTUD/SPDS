package test.cases.statics;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class StaticFieldFlows extends AbstractBoomerangTest {
	private static Object alloc;
	private static Alloc instance;
	@Test
	public void simple(){
		alloc = new Alloc();
		Object alias = alloc;
		queryFor(alias);
	}
	@Test
	public void singleton(){
		Alloc singleton = StaticFieldFlows.v();
		Object alias = singleton;
		queryFor(alias);
	}
	

	@Test
	public void doubleUnbalancedSingleton(){
		Alloc singleton = returns();
		Object alias = singleton;
		queryFor(alias);
	}
	
	private static Alloc returns() {
		return StaticFieldFlows.v();
	}
	
	private static Alloc v() {
		if(instance == null)
			instance = new Alloc();
		Alloc loaded = instance;
		return loaded;
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
