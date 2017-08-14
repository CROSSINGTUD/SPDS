package test.cases.sets;

import java.util.Iterator;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class CustomSetTest extends AbstractBoomerangTest {

	@Test
	public void mySetIteratorTest() {
		MySetIterator mySet = new MySetIterator();
		AllocatedObject alias = new AllocatedObject() {
		};
		mySet.add(alias);
		Object query = null;
		while (mySet.hasNext()) {
			query = mySet.next();
		}
		queryFor(query);
	}

	private class MySetIterator implements Iterator<Object> {
		Object[] content = new Object[10];
		int curr = 0;

		void add(Object o) {
			content[o.hashCode()] = o;
		}

		@Override
		public boolean hasNext() {
			return curr < 10;
		}

		@Override
		public Object next() {
			return content[curr++];
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub

		}
	}

	@Test
	public void mySetIterableTest() {
		MySet mySet = new MySet();
		AllocatedObject alias = new AllocatedObject() {
		};
		mySet.add(alias);
		Object query = null;
		for (Object el : mySet) {
			query = el;
		}
		queryFor(query);
	}

	private class MySet implements Iterable<Object> {
		Object[] content = new Object[10];
		int curr = 0;
		Iterator<Object> it;

		void add(Object o) {
			content[o.hashCode()] = o;
		}

		@Override
		public Iterator<Object> iterator() {
			if (it == null)
				it = new SetIterator();
			return it;
		}

		private class SetIterator implements Iterator<Object> {

			@Override
			public boolean hasNext() {
				return curr < 10;
			}

			@Override
			public Object next() {
				return content[curr++];
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub

			}
		}
	}
}
