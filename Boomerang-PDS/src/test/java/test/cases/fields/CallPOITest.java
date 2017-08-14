package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class CallPOITest extends AbstractBoomerangTest {
	private static class A{
		B b = new B();
	}
	private static class B{
		C c;
	}
	private static class C implements AllocatedObject{
		
	}

	private static void allocation(A a) {
		B intermediate = a.b;
		C d = new C();
		intermediate.c = d;
	}
	
	@Test
	public void indirectAllocationSite(){
		A a = new A();
		allocation(a);
		C alias = a.b.c;
		queryFor(alias);
	}

	@Test
	public void indirectAllocationSiteViaParameter(){
		A a = new A();
		C alloc = new C();
		allocation(a,alloc);
		C alias = a.b.c;
		queryFor(alias);
	}

	@Test
	public void indirectAllocationSiteViaParameterAliased(){
		A a = new A();
		C alloc = new C();
		A b = a;
		allocation(a,alloc);
		C alias = b.b.c;
		queryFor(alias);
	}
	

	@Test
	public void whyRecursiveCallPOIIsNecessary(){
		A a = new A();
		C alloc = new C();
		A b = a;
		allocationIndirect(a,alloc);
		C alias = b.b.c;
		queryFor(alias);
	}
	private void allocation(A a, C d) {
		B intermediate = a.b;
		intermediate.c = d;
	}
	private void allocationIndirect(A a, C d) {
		B b = new B();
		A a2 = a;
		a2.b= b;
		B intermediate = a2.b;
		intermediate.c = d;
	}
}
