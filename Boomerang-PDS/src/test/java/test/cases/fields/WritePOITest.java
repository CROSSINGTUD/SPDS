package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class WritePOITest extends AbstractBoomerangTest {
	private class A{
		Alloc b = null;
	}
	
	
	@Test
	public void indirectAllocationSite(){
		Alloc query = new Alloc();
		A a = new A();
		A e = a;
		a.b = query;
		Alloc alias = e.b;
		queryFor(query);
	}
	
	private class Alloc implements AllocatedObject{};
}
