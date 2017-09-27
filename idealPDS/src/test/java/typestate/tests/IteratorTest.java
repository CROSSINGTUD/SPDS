package typestate.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.ConcreteState;
import typestate.TypestateChangeFunction;
import typestate.impl.statemachines.HasNextStateMachine;

public class IteratorTest extends IDEALTestingFramework {


	@Test
	public void test1() {
		List<Object> list = new LinkedList<>();
		list.add(new Object());
		list.add(new Object());
		for (Object l : list) {
			System.out.println(l);
		}
		mustBeInAcceptingState(list.iterator());
	}

	@Test
	public void test2() {
		MyLinkedList<Object> list = new MyLinkedList<>();
		list.add(new Object());
		java.util.Iterator<Object> iterator = list.iterator();
		iterator.hasNext();
		iterator.next();
		iterator.next();
		mustBeInErrorState(iterator);
	}

	@Test
	public void test3() {
		LinkedList<Object> list = new LinkedList<>();
		list.add(new Object());
		Iterator it1 = list.iterator();
		Object each = null;
		for (; it1.hasNext(); each = it1.next()) {
			try {
				each.toString();
			} catch (Throwable e) {
				e.getMessage();
			}
		}
		mustBeInAcceptingState(it1);
	}

	@Test
	public void test4() {
		List l1 = new ArrayList();
		List l2 = new ArrayList();

		l1.add("foo");
		l1.add("moo");
		l1.add("zoo");

		Object v;
		Iterator it1 = l1.iterator();
		for (; it1.hasNext(); v = it1.next()) {
			System.out.println(foo(it1));
		}
		mayBeInErrorState(it1);
	}

	public Object foo(Iterator it) {
		return it.next();
	}

	private static class MyLinkedList<V> {

		public void add(Object object) {
			// TODO Auto-generated method stub

		}

		public Iterator<V> iterator() {
			return new MyIterator<V>();
		}

	}

	private static class MyIterator<V> implements Iterator<V> {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public V next() {
			return null;
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
		}

	}

	@Override
	protected TypestateChangeFunction<ConcreteState> createTypestateChangeFunction() {
		return new HasNextStateMachine();
	}
}