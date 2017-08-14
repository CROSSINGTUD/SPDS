package test.cases.array;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class ArrayTest extends AbstractBoomerangTest {

	public static class A implements AllocatedObject {
	}

	@Test
	public void simpleAssignment() {
		Object[] array = new Object[3];
		A alias = new A();
		array[1] = alias;
		Object query = array[2];
		queryFor(query);
	}

	@Test
	public void indexInsensitive() {
		Object[] array = new Object[3];
		A alias1 = new A();
		A alias2 = new A();
		array[1] = alias1;
		array[2] = alias2;
		Object query = array[2];
		queryFor(query);
	}

	@Test
	public void doubleArray() {
		Object[][] array = new Object[3][3];
		array[1][2] = new A();
		array[2][3] = new Object();
		Object query = array[3][3];
		queryFor(query);
	}

	@Test
	public void threeDimensionalArray() {
		Object[][][] array = new Object[3][3][1];
		array[1][2][1] = new A();
		array[2][3][0] = new Object();
		Object query = array[3][3][2];
		queryFor(query);
	}

	@Test
	public void arrayCopyTest() {
		Object[] copiedArray = new Object[3];
		Object[] originalArray = new Object[3];
		A alias = new A();
		originalArray[1] = alias;
		System.arraycopy(originalArray, 0, copiedArray, 0, 1);
		Object query = copiedArray[3];
		queryFor(query);
	}

	@Test
	public void arrayWithTwoObjectAndFieldTest() {
		B b = new B();
		b.f = new A();
		C a = new C();
		a.f = new Object();
		Object[] container = new Object[2];
		container[0] = b;
		container[1] = a;
		Object bAlias = container[0];
		B casted = (B) bAlias;
		Object query = casted.f;
		queryFor(query);
	}

	@Test
	public void toCharArrayTest() {
		String s = "password";
		char[] query = s.toCharArray();
		char[] t = query;
		queryFor(t);
	}

	private static class B {
		Object f;
	}

	private static class C {
		Object f;
	}
}
