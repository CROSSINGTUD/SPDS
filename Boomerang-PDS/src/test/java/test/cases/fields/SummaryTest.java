package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class SummaryTest extends AbstractBoomerangTest{

	@Test
	public void branchedSummaryReuse(){
		A x = new A();
		B query = null;
		if(staticallyUnknown()){
			x.f = new B();
			query = load(x);
		}else{
			x.f = new B();
			query = load(x);
		}
		queryFor(query);
	}
	
	private B load(A x) {
		return x.f;
	}

	private class A {
		B f;
	}
	private class B implements AllocatedObject{}
}
