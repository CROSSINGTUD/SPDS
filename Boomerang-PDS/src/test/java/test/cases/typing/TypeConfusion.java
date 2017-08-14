package test.cases.typing;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class TypeConfusion extends AbstractBoomerangTest{
	@Test
	public void invokesInterface(){
		B b = new B();
		A a1 = new A();
		Object o = b;
		A a = null;
		if(staticallyUnknown()){
			a = a1;
		} else{
			a = (A)o;
		} 
		queryFor(a);
	}
	

	
	private static class A implements AllocatedObject{
	}	
	private static class B {
	}
	
}
