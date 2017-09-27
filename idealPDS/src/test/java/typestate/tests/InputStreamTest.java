package typestate.tests;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Ignore;
import org.junit.Test;

import ideal.debug.IDebugger;
import ideal.debug.NullDebugger;
import test.IDEALTestingFramework;
import test.slowmethod.SlowMethodDetector;
import typestate.ConcreteState;
import typestate.TypestateChangeFunction;
import typestate.TypestateDomainValue;
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
	protected TypestateChangeFunction<ConcreteState> createTypestateChangeFunction() {
		return new InputStreamStateMachine();
	}
	
	@Override
	protected IDebugger<TypestateDomainValue<ConcreteState>> getDebugger() {
		return new NullDebugger<TypestateDomainValue<ConcreteState>>();
	}
}