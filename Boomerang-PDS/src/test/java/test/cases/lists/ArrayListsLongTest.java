package test.cases.lists;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class ArrayListsLongTest extends AbstractBoomerangTest{
	@Test
	public void addAndRetrieveWithIterator(){
		List<Object> set = new ArrayList<Object>();
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
		List<Object> list = new ArrayList<Object>();
		AllocatedObject alias = new AllocatedObject(){};
		list.add(alias);
		Object ir = list.get(0);
		Object query2 = ir;
		queryFor(query2);
	}
	@Test
	public void addAndRetrieveByIndex2(){
		List<Object> list = new ArrayList<Object>();
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
