package test.cases.fields.loops;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class LoopsWithFieldsIntraTest extends AbstractBoomerangTest{
	@Test
	public void oneFields() {
		Node x = new Node();
		Node p = null;
		while(staticallyUnknown()){
			if(staticallyUnknown()){
				x.left = p;
				
			}else if(staticallyUnknown()){
				x.right = p;
			}
			p = x;
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
	@Test
	public void twoFields() {
		Node x = new Node();
		Node p = null;
		while(staticallyUnknown()){
			if(staticallyUnknown()){
				x.left.right = p;
				
			}else if(staticallyUnknown()){
				x.right.left = p;
			}
			p = x;
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
	public void oneFieldSimpleLoop() {
		Node x = new Node();
		while(staticallyUnknown()){
			x.left = x;
		}
		Node h = x.left;
		queryFor(h);
	}

	@Test
	public void twoFieldSimpleLoop() {
		Node x = new Node();
		while(staticallyUnknown()){
			x.left.right = x;
		}
		Node h = x.left.right;
		queryFor(h);
	}
	@Test
	public void twoFieldSimpleLoopWithBranched() {
		Node x = new Node();
		while(staticallyUnknown()){
			if(staticallyUnknown()){
				x.left.right = x;
			}else{
				x.right.left = null;	
			}
		}
		Node h = x.left.right;
		queryFor(h);
	}
	
	@Test
	public void threeFields() {
		TreeNode x = new TreeNode();
		TreeNode p = null;
		while(staticallyUnknown()){
			if(staticallyUnknown()){
				x.left.right = p;
				
			}else if(staticallyUnknown()){
				x.right.left = p;
			} else{
				TreeNode u = x.parent;
				x = u;
			}
			p = x;
		}
		TreeNode t;
		if(staticallyUnknown()){
			t = x.left.right;
			
		}else{
			t = x.right.left;
		}
		TreeNode h = t;
		queryFor(h);
	}
	private class Node implements AllocatedObject{
		Node left = new Node();
		Node right = new Node();
	}

	private class TreeNode implements AllocatedObject{
		TreeNode left = new TreeNode();
		TreeNode right = new TreeNode();
		TreeNode parent = new TreeNode();
	}
}
