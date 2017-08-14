package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class WritePOITest extends AbstractBoomerangTest {
	private class A{
		Alloc b = null;
		Alloc c = null;
	}
	
	
	@Test
	public void indirectAllocationSite(){
		Alloc query = new Alloc();
		A a = new A();
		A e = a;
		a.b = query;
		Alloc alias = e.b;
		queryFor(alias);
	}

	
	@Test
	public void directAllocationSite(){
		Alloc query = new Alloc();
		A a = new A();
		a.b = query;
		Alloc alias = a.b;
		queryFor(alias);
	}
	private class Alloc implements AllocatedObject{};
}
