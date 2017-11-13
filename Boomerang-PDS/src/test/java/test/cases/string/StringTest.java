package test.cases.string;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class StringTest extends AbstractBoomerangTest {
	@Test
	public void stringConcat(){
		Object query = "a" + "b";
		if(staticallyUnknown())
			query += "c";
		System.out.println(query);
		queryFor(query);
	}

}
