package typestate.tests;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.finiteautomata.MatcherStateMachine;
import typestate.impl.statemachines.InputStreamStateMachine;

public class InputStreamTest extends IDEALTestingFramework {

	@Test
	public void test1() throws IOException {
		InputStream inputStream = new FileInputStream("");
		inputStream.close();
		inputStream.read();
		mustBeInErrorState(inputStream);
	}

	@Test
	public void test2() throws IOException {
	    InputStream inputStream = new FileInputStream("");
	    inputStream.close();
	    inputStream.close();
	    inputStream.read();
	    mustBeInErrorState(inputStream);
	}

	@Test
	public void test3() throws IOException {
	    InputStream inputStream = new FileInputStream("");
	    inputStream.read();
	    inputStream.close();
	    mustBeInAcceptingState(inputStream);
	}

	@Override
	protected MatcherStateMachine getStateMachine() {
		return new InputStreamStateMachine();
	}
}