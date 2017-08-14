package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;

public class NullAllocationConstructorTest extends AbstractBoomerangTest{
	private class A{
		B f = null;
	}
	private class B{
		
	}
	@Test
	public void nullAllocationOfField(){
		A a = new A();
		B variable = a.f;
		queryFor(variable);
	}
}
