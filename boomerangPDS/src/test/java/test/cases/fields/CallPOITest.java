package test.cases.fields;

import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class CallPOITest extends AbstractBoomerangTest {
	private static class A{
		B b = new B();
	}
	private static class B{
		C c = new C();
	}
	private static class C implements AllocatedObject{
		
	}

	@Test
	public void simpleButDiffer() {
		Alloc c = new Alloc();
		T t = new T(c);
		S s = new S();
		t.foo(s);
		Object q = s.get();
		queryFor(q);
	}

	public static class T{
		private Object value;

		T(Object o){
			this.value = o;
		}
		public void foo(S s){
			Object val = this.value;
			s.set(val);
		}
		
	}

	public static class S{
		private Object valueInS;

		public void set(Object val){
			this.valueInS = val;
		}
		public Object get(){
			Object alias = this.valueInS;
			return alias;
		}
	}
	private static void allocation(A a) {
		B intermediate = a.b;
		C e = new C();
		C d = e;
		intermediate.c = d;
	}
	
	@Test
	public void indirectAllocationSite3Address(){
		A a = new A();
		allocation(a);
		B load = a.b;
		C alias = load.c;
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
//		a.b = new B();
		C alloc = new C();
		A b = a;
		allocation(a,alloc);
		B loadedFromB = b.b;
		C alias = loadedFromB.c;
		queryFor(alias);
	}

	private void allocation(A a, C d) {
		B intermediate = a.b;
		intermediate.c = d;
	}

	@Test
	public void whyRecursiveCallPOIIsNecessary(){
		//TODO This test case seems to be non deterministic, why?
		A a = new A();
		C alloc = new C();
		A b = a;
		allocationIndirect(a,alloc);
		B loadedFromB = b.b;
		C alias = loadedFromB.c;
		queryFor(alias);
	}
	@Test
	public void whyRecursiveCallPOIIsNecessarySimpler(){
		A a = new A();
		C alloc = new C();
		allocationIndirect(a,alloc);
		C alias = a.b.c;
		queryFor(alias);
	}
	private void allocationIndirect(A a, C d) {
		B b = new B();
		A a2 = a;
		a2.b = b;
		B intermediate = a2.b;
		intermediate.c = d;
		int x=1;
	}
}
