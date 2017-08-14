package test.cases.sets;

import java.util.HashSet;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class HashSetsLongTest extends AbstractBoomerangTest{
	@Test
	public void addAndRetrieve(){
		HashSet<Object> set = new HashSet<>();
		AllocatedObject alias = new AllocatedObject(){};
		AllocatedObject alias3 = new AllocatedObject(){};
		set.add(alias);
		set.add(alias3);
		Object alias2 = null;
		for(Object o : set)
			alias2 = o;
		Object ir = alias2;
		Object query2 = ir;
		queryFor(query2);
	}
	
	@Override
	protected boolean includeJDK() {
		return true;
	}
}
