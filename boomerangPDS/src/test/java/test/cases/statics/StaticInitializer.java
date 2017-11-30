package test.cases.statics;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class StaticInitializer extends AbstractBoomerangTest {
	private static Object alloc = new Alloc();
	
	@Test
	public void doubleSingleton(){
		System.out.println(2);
		Object alias = alloc;
		queryFor(alias);
	}
}
