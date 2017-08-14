package test.cases.sets;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class HashMapsLongTest extends AbstractBoomerangTest{
	@Test
	public void addAndRetrieve(){
		Map<Object,Object> set = new HashMap<>();
		Object key = new Object();
		AllocatedObject alias3 = new AllocatedObject(){};
		set.put(key,alias3);
		Object alias2 = null;
		for(Object o : set.values())
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
