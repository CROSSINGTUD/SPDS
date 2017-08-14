package test.cases.basic;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class FieldlessTest extends AbstractBoomerangTest {

	public class Allocation implements AllocatedObject {

	}

	@Test
	public void simpleAssignment1() {
		Object alloc1 = new Allocation();
		Object alias1 = alloc1;
		Object query = alias1;
		queryFor(query);
	}

	@Test
	public void simpleAssignment2(){
	    Object alias1 = new AllocatedObject(){}, b, c, alias2, alias3;
	    alias2 = alias1;
	    c = new Object();
	    alias3 = alias1;
	    queryFor(alias3);
	}
	@Test
	public void branchWithOverwrite() {
		Object alias2 = new Allocation();
		if (staticallyUnknown()) {
			Object alias1 = alias2;
			alias2 = new Allocation();
		}

		queryFor(alias2);
	}
	@Test
	public void branchWithOverwriteSwapped() {
		Object alias2 = new Allocation();
		Object alias1 = new Allocation();
		if (staticallyUnknown()) {
			alias2 = alias1;
		}

		queryFor(alias2);
	}
	@Test
	public void returnNullAllocation() {
		Object alias2 = returnNull(); 
		queryFor(alias2);
	}

	private Object returnNull() {
		Object x = new Object();
		return null;
	}

	@Test
	public void cast() {
		Allocation alias1 = new Subclass();
		Subclass alias2 = (Subclass) alias1;
		queryFor(alias2);
	}
	
	private class Subclass extends Allocation{}
	
	public AllocatedObject create() {
		AllocatedObject alloc1 = new AllocatedObject() {
		};
		return alloc1;
	}

}
