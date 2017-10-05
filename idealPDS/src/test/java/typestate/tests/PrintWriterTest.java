package typestate.tests;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.finiteautomata.MatcherStateMachine;
import typestate.impl.statemachines.PrintWriterStateMachine;

public class PrintWriterTest extends IDEALTestingFramework {

	@Test
	public void test1() throws FileNotFoundException {
		PrintWriter inputStream = new PrintWriter("");
		inputStream.close();
		inputStream.flush();
		mustBeInErrorState(inputStream);
	}

	@Override
	protected MatcherStateMachine getStateMachine() {
		return new PrintWriterStateMachine();
	}

}