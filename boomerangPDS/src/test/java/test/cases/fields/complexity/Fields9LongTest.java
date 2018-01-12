package test.cases.fields.complexity;

import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class Fields9LongTest extends AbstractBoomerangTest {
	@Test
	public void test() {
		TreeNode x = new TreeNode();
		TreeNode p = null;
		while (staticallyUnknown()) {
			if (staticallyUnknown()) {
				x.a = p;
			}
			if (staticallyUnknown()) {
				x.b = p;
			}
			if (staticallyUnknown()) {
				x.c = p;
			}
			if (staticallyUnknown()) {
				x.d = p;
			}
			if (staticallyUnknown()) {
				x.e = p;
			}
			if (staticallyUnknown()) {
				x.f = p;
			}
			if (staticallyUnknown()) {
				x.g = p;
			}
			if (staticallyUnknown()) {
				x.h = p;
			}
			if (staticallyUnknown()) {
				x.i = p;
			}
			p = x;
		}
		TreeNode t = null;
		if (staticallyUnknown()) {
			t = x.a;
		}
		if (staticallyUnknown()) {
			t = x.b;
		}
		if (staticallyUnknown()) {
			t = x.c;
		}
		if (staticallyUnknown()) {
			t = x.d;
		}
		if (staticallyUnknown()) {
			t = x.e;
		}
		if (staticallyUnknown()) {
			t = x.f;
		}

		if (staticallyUnknown()) {
			t = x.g;
		}
		if (staticallyUnknown()) {
			t = x.h;
		}
		if (staticallyUnknown()) {
			t = x.i;
		}
		TreeNode h = t;
		queryFor(h);
	}

	private class TreeNode implements AllocatedObject {
		TreeNode a = new TreeNode();
		TreeNode b = new TreeNode();
		TreeNode c = new TreeNode();
		TreeNode d = new TreeNode();
		TreeNode e = new TreeNode();
		TreeNode f = new TreeNode();
		TreeNode g = new TreeNode();
		TreeNode h = new TreeNode();
		TreeNode i = new TreeNode();
	}
}
