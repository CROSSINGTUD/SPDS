package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class ReadPOITest extends AbstractBoomerangTest {
	private class A{
		Alloc b = null;
	}
	
	
	@Test
	public void indirectAllocationSite(){
		A a = new A();
		A e = a;
		e.b = new Alloc();
		Alloc query = a.b;
		queryFor(query);
	}
	
	

	@Test
	public void indirectAllocationSiteTwoFields(){
		Node a = new Node();
		a.left.right = new AllocNode();
		Node query = a.left.right;
		queryFor(query);
	}
	@Test
	public void twoFieldsBranched(){
		Node a = new Node();
		init(a);
		Node query = null;
		if(staticallyUnknown())
			query = a.left;
		else
			query = a.right;
		queryFor(query);
	}
	

	private void init(Node a) {
		a.left = new AllocNode();
		a.right = new AllocNode();		
	}



	@Test
	public void overwriteFieldWithItself(){
		List query = new List();
		query = query.next;
		queryFor(query);
	}

	private class List{
		List next = new AllocListElement();
	}
	private class AllocListElement extends List implements AllocatedObject{}
	private class Node{
		Node left = new Node();
		Node right;
	}
	private class AllocNode extends Node implements AllocatedObject{}
	private class Alloc implements AllocatedObject{};
}
