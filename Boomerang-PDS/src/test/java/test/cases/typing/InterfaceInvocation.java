package test.cases.typing;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class InterfaceInvocation extends AbstractBoomerangTest{
	@Test
	public void invokesInterface(){
		B b = new B();
		wrappedFoo(b);
		A a1 = new A();
		A a2 = new A();
		A a = null;
		if(staticallyUnknown()){
			a = a1;
		} else{
			a = a2;
		}
		wrappedFoo(a);
		queryFor(a);
	}
	
	private void wrappedFoo(I a) {
		foo(a);
	}

	private void foo(I a) {
		a.bar();
	}

	private static interface I{
		void bar();
	}

	
	private static class A implements I, AllocatedObject{
		@Override
		public void bar() {
		}
	}	
	private static class B implements I{

		@Override
		public void bar() {
		}
	}
	
	@Override
	protected Collection<String> errorOnVisitMethod() {
		return Collections.singleton("<test.cases.typing.InterfaceInvocation$B: void bar()>");
	}
	
}
