package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class IntraproceduralStrongUpdateTest extends AbstractBoomerangTest{
	@Test
	public void strongUpdateWithField(){
		A a = new A();
		a.field = new Object();
		A b = a;
		b.field = new AllocatedObject(){};
		Object alias = a.field;
		queryFor(alias);
	}
	@Test
	public void strongUpdateWithFieldSwapped(){
		A a = new A();
		A b = a;
		b.field = new Object();
		a.field = new AllocatedObject(){};
		Object alias = a.field;
		queryFor(alias);
	}
	private class A{
		Object field;
	}
}
