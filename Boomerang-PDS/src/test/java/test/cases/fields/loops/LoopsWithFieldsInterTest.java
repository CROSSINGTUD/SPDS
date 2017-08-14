package test.cases.fields.loops;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class LoopsWithFieldsInterTest extends AbstractBoomerangTest{
	@Test
	public void twoFields() {
		Node x = new Node();
		Node p = null;
		while(staticallyUnknown()){
			if(staticallyUnknown()){
				leftOf(x).right = p;
				
			}else{
				rightOf(x).left = p;
			}
			p = x;
		}
		Node t;
		if(staticallyUnknown()){
			t = rightOf(leftOf(x));
			
		}else{
			t = leftOf(rightOf(x));
		}
		Node h = t;
		queryFor(h);
	}
	private Node leftOf(Node x) {
		return x == null ? x.left  : null;
	}
	private Node rightOf(Node x) {
		return x == null ? x.right  : null;
	}
	private TreeNode leftOf(TreeNode x) {
		return x == null ? x.left  : null;
	}
	private TreeNode rightOf(TreeNode x) {
		return x == null ? x.left  : null;
	}
	private TreeNode parentOf(TreeNode x) {
		return x == null ? x.parent  : null;
	}
	@Test
	public void threeFields() {
		TreeNode x = new TreeNode();
		TreeNode p = null;
		while(staticallyUnknown()){
			if(staticallyUnknown()){
				leftOf(x).right = p;
				
			}else if(staticallyUnknown()){
				rightOf(x).left = p;
			} else{
				TreeNode u = parentOf(x);
				x = u;
			}
			p = x;
		}
		TreeNode t;
		if(staticallyUnknown()){
			t = rightOf(leftOf(x));
			
		}else{
			t = leftOf(rightOf(x));
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
