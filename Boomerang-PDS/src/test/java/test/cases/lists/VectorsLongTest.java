package test.cases.lists;


import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class VectorsLongTest extends AbstractBoomerangTest{
	@Test
	public void addAndRetrieveWithIterator(){
		List<Object> set = new Vector<Object>();
		AllocatedObject alias = new AllocatedObject(){};
		set.add(alias);
		Object alias2 = null;
		for(Object o : set)
			alias2 = o;
		Object ir = alias2;
		Object query2 = ir;
		queryFor(query2);
	}
	@Test
	public void addAndRetrieveByIndex1(){
		List<Object> list = new Vector<Object>();
		AllocatedObject alias = new AllocatedObject(){};
		list.add(alias);
		Object ir = list.get(0);
		Object query2 = ir;
		queryFor(query2);
	}
	@Test
	public void addAndRetrieveByIndex2(){
		List<Object> list = new Vector<Object>();
		AllocatedObject alias = new AllocatedObject(){};
		list.add(alias);
		Object ir = list.get(1);
		Object query2 = ir;
		queryFor(query2);
	}
	@Override
	protected boolean includeJDK() {
		return true;
	}
}
