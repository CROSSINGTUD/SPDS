package test.cases.fields.complexity;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class Fields6LongTest extends AbstractBoomerangTest{
	@Test
	public void test() {
		TreeNode x = new TreeNode();
		TreeNode p = null;
		while(staticallyUnknown()){
			if(staticallyUnknown()){
				x.a = p;
			}
			if(staticallyUnknown()){
				x.b = p;
			}
			if(staticallyUnknown()){
				x.c = p;
			}
			if(staticallyUnknown()){
				x.d = p;
			}
			if(staticallyUnknown()){
				x.e = p;
			}
			if(staticallyUnknown()){
				x.f = p;
			}
			p = x;
		}
		TreeNode t = null;
		if(staticallyUnknown()){
			t = x.a;
		}
		if(staticallyUnknown()){
			t = x.b;
		}
		if(staticallyUnknown()){
			t = x.c;
		}
		if(staticallyUnknown()){
			t = x.d;
		}
		if(staticallyUnknown()){
			t = x.e;
		}
		if(staticallyUnknown()){
			t = x.f;
		}
		TreeNode h = t;
		queryFor(h);
	}

	private class TreeNode implements AllocatedObject{
		TreeNode a = new TreeNode();
		TreeNode b = new TreeNode();
		TreeNode c = new TreeNode();
		TreeNode d = new TreeNode();
		TreeNode e = new TreeNode();
		TreeNode f = new TreeNode();
	}
}
