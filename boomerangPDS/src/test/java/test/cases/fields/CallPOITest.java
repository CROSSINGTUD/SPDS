package test.cases.fields;

import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class CallPOITest extends AbstractBoomerangTest {
	private static class A{
		B b = new B();
	}
	private static class B{
		AllocObj c = new AllocObj();
	}
	private static class AllocObj implements AllocatedObject{
		
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
		AllocObj e = new AllocObj();
		AllocObj d = e;
		intermediate.c = d;
	}
	
	@Test
	public void indirectAllocationSite3Address(){
		A a = new A();
		allocation(a);
		B load = a.b;
		AllocObj alias = load.c;
		queryFor(alias);
	}

	@Test
	public void indirectAllocationSiteViaParameter(){
		A a = new A();
		AllocObj alloc = new AllocObj();
		allocation(a,alloc);
		AllocObj alias = a.b.c;
		queryFor(alias);
	}

	@Test
	public void indirectAllocationSiteViaParameterAliased(){
		A a = new A();
//		a.b = new B();
		AllocObj alloc = new AllocObj();
		A b = a;
		allocation(a,alloc);
		B loadedFromB = b.b;
		AllocObj alias = loadedFromB.c;
		queryFor(alias);
	}

	private void allocation(A a, AllocObj d) {
		B intermediate = a.b;
		intermediate.c = d;
	}

	@Test
	public void whyRecursiveCallPOIIsNecessary(){
		//TODO This test case seems to be non deterministic, why?
		A a = new A();
		AllocObj alloc = new AllocObj();
		A b = a;
		allocationIndirect(a,alloc);
		B loadedFromB = b.b;
		AllocObj alias = loadedFromB.c;
		queryFor(alias);
	}
	@Test
	public void whyRecursiveCallPOIIsNecessarySimpler(){
		A a = new A();
		AllocObj alloc = new AllocObj();
		allocationIndirect(a,alloc);
		AllocObj alias = a.b.c;
		queryFor(alias);
	}
	private void allocationIndirect(A a, AllocObj d) {
		B b = new B();
		A a2 = a;
		a2.b = b;
		B intermediate = a2.b;
		intermediate.c = d;
		AllocObj AndMe = a.b.c;
	}
	
	@Test
	public void innerSetFieldOnAlias(){
		Outer o = new Outer();
		set(o);
		Object alias = o.field;
		queryFor(alias);
	}
	private void set(Outer o) {
		Alloc alloc = new Alloc();
		Outer alias = o;
		alias.field = alloc;
	}

	public static class Outer{
		Object field;
	}
}
