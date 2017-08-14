package test.cases.subclassing;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class InnerClass2Test extends AbstractBoomerangTest {
	public void doThings(final Object name) {
		class MyInner {
			public void seeOuter() {
				queryFor(name);
			}
		}
		MyInner inner = new MyInner();
		inner.seeOuter();
	}
	@Test
	public void run(){
		Object alloc = new Allocation();
		String cmd = System.getProperty("");
		if(cmd!=null){
			alloc = new Allocation();
		}
		InnerClass2Test outer = new InnerClass2Test();
		outer.doThings(alloc);
	}
	private class Allocation implements AllocatedObject{
		
	}
}
