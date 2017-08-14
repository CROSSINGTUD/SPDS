package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class FieldsBranchedTest extends AbstractBoomerangTest{
	@Test
	public void twoFieldsNoLoop() {
		Node x = new Node();
		if(staticallyUnknown()){
			x.left.right = x;
		}else if(staticallyUnknown()){
			x.right.left = x;
		}
		Node t;
		if(staticallyUnknown()){
			t = x.left.right;
		}else{
			t = x.right.left;
		}
		Node h = t;
		queryFor(h);
	}
	@Test
	public void twoFieldsNoLoop2() {
		Node x = new Node();
		Node t = null;
		if(staticallyUnknown()){
			x.left.right = x;
			t = x.left.right;
		}else if(staticallyUnknown()){
			x.right.left = x;
			t = x.right.left;
		}
		Node h = t;
		queryFor(h);
	}
	@Test
	public void oneFieldsNoLoop() {
		Node x = new Node();
		if(staticallyUnknown()){
			x.left = x;
		}else if(staticallyUnknown()){
			x.right = x;
		}
		Node t;
		if(staticallyUnknown()){
			t = x.left;
		}else{
			t = x.right;
		}
		Node h = t;
		queryFor(h);
	}
	private class Node implements AllocatedObject{
		Node left = new Node();
		Node right = new Node();
	}

}
