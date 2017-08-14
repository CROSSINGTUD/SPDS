package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class ReturnPOITest extends AbstractBoomerangTest {
	private class A{
		B b;
	}
	private class B {
		C c;
	}
	private class C implements AllocatedObject{
	}
	
	
	@Test
	public void indirectAllocationSite(){
		B a = new B();
		B e = a;
		allocation(a);
		C alias = e.c;
		C query = a.c;
		queryFor(query);
	}

	private void allocation(B a) {
		C d = new C();
		a.c = d;
	}
	
	@Test
	public void unbalancedReturnPOI1(){
		C a = new C();
		B b =  new B();
		B c = b;
		setField(b,a);
		C alias = c.c;
		queryFor(a);
	}

	private void setField(B a2, C a) {
		a2.c = a;
	}

	@Test
	public void unbalancedReturnPOI3(){
		B b =  new B();
		B c = b;
		setField(c);
		C query = c.c;
		queryFor(query);
	}
	private void setField(B c) {
		c.c = new C();
	}

	@Test
	public void whyRecursiveReturnPOIIsNecessary(){
		C c = new C();
		B b =  new B();
		A a = new A();
		A a2 = a;
		a2.b = b;
		B b2 = b;
		setFieldTwo(a,c);
		C alias = a2.b.c;
		queryFor(c);
	}

	private void setFieldTwo(A b, C a) {
		b.b.c = a;
	}
}
