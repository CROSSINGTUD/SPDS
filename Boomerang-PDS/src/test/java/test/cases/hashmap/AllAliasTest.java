package test.cases.hashmap;

import org.junit.Ignore;
import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;


@Ignore
public class AllAliasTest extends AbstractBoomerangTest{
	@Test
	public void test() {
		TreeNode<Object,Object> a = new TreeNode<Object, Object>(0, new Object(), new Object(), null);
		TreeNode<Object, Object> t = new TreeNode<Object, Object>(0, null, new Object(), a);
		t.balanceDeletion(t, a);
//		t.balanceInsertion(t, t);
		t.treeify(new TreeNode[]{a,t});
//		t.moveRootToFront(new TreeNode[]{a,t},a);
		queryFor(t);
	}

}
