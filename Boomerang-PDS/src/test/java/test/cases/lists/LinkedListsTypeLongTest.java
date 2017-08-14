package test.cases.lists;


import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class LinkedListsTypeLongTest extends AbstractBoomerangTest{
	
	@Override
	protected boolean includeJDK() {
		return true;
	}
	
	@Test
	public void addAndRetrieveWithIteratorWithTyping(){
		List<I> list2 = new LinkedList<I>();
		B b = new B();
		list2.add(b);
		List<I> list1 = new LinkedList<I>();
		A alias = new A();
		list1.add(alias);
		I alias2 = null;
		for(I o : list1)
			alias2 = o;
		I ir = alias2;
		I query2 = ir;
		query2.bar();
		queryFor(query2);
	}
	
	private static class A implements I, AllocatedObject{

		@Override
		public void bar() {
			// TODO Auto-generated method stub
			
		}
		
	}
	private static class B implements I{

		@Override
		public void bar() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private interface I{
		void bar();
	}
	
	@Override
	protected Collection<String> errorOnVisitMethod() {
		return Collections.singleton("<test.cases.lists.LinkedListsTypeLongTest$B: void bar()>");
	}
}
