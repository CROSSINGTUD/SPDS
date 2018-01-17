package test.cases.statics;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class StaticWithSuperclasses extends AbstractBoomerangTest {
	@Test
	public void simple(){
		List list = new List();
		Object o = list.get();
		queryForAndNotEmpty(o);
	}

	private static class List {
		
		private static Object elementData = new Alloc();
		public Object get() {
			return elementData;
		}
	}
	
	@Test
	public void supclass(){
		MyList list = new MyList();
		Object o = list.get();
		queryForAndNotEmpty(o);
	}

	private static class MyList extends List {
		
		private static Object elementData2 = new Alloc();
		public Object get() {
			return elementData2;
		}
	}
}
