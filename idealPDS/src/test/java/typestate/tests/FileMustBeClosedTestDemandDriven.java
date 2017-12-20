package typestate.tests;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.FileMustBeClosedStateMachine;
import typestate.test.helper.File;
import typestate.test.helper.ObjectWithField;

public class FileMustBeClosedTestDemandDriven extends IDEALTestingFramework{
	@Test
	public void notCaughtByCHA() {
		File file = new File();
		file.open();
		B b = new B();
		b.flow(file);
		mustBeInAcceptingState(file);
	}

	@Test
	public void notCaughtByRTA() {
		File file = new File();
		file.open();
		A a = new A();
		B b = new B();
		b.flow(file);
		mustBeInAcceptingState(file);
	}

	private interface I{
		void flow(File f);
	}

	private static class B implements I{
		@Override
		public void flow(File f) {
			f.close();
		}
	}

	private static class A implements I{
		@Override
		public void flow(File f) {
			shouldNotBeAnalyzed();
		}
	}

	@Override
	protected TypeStateMachineWeightFunctions getStateMachine() {
		return new FileMustBeClosedStateMachine();
	}
}
