package test.cases.integers;

import org.junit.Test;

import test.core.AbstractBoomerangTest;

public class IntTest extends AbstractBoomerangTest {
	@Test
	public void simpleAssign(){
		int allocation = 1;
		intQueryFor(allocation);
	}

	@Test
	public void byteArrayLength(){
		byte[] array = new byte[1];
		int length = array.length;
		intQueryFor(length);
	}
	@Test
	public void simpleIntraAssign(){
		int allocation = 1;
		int y = allocation;
		intQueryFor(y);
	}

	@Test
	public void simpleInterAssign(){
		int allocation = 1;
		int y = foo(allocation);
		intQueryFor(y);
	}
	@Test
	public void returnDirect(){
		int allocation = getVal();
		intQueryFor(allocation);
	}

	@Test
	public void returnInDirect(){
		int x = getValIndirect();
		intQueryFor(x);
	}
	private int getValIndirect() {
		int allocation = 1;
		return allocation;
	}
	private int getVal() {
		return 1;
	}
	private int foo(int x) {
		int y = x;
		return y;
	}
}
