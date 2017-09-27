package typestate.tests;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.ConcreteState;
import typestate.TypestateChangeFunction;
import typestate.impl.statemachines.FileMustBeClosedStateMachine;
import typestate.test.helper.File;
@SuppressWarnings("deprecation")
public class FileMustBeClosedInterfaceTest extends IDEALTestingFramework {
	@Test
	public void main() {
		FileMustBeClosedInterfaceTest target11 = new FileMustBeClosedInterfaceTest();
		File file = new File();
		Flow flow = (staticallyUnknown() ? target11.new ImplFlow1() : target11.new ImplFlow2());
		flow.flow(file);
		mayBeInErrorState(file);
		mayBeInAcceptingState(file);
	}

	public class ImplFlow1 implements Flow {
		@Override
		public void flow(File file) {
			file.open();
		}

	}

	public class ImplFlow2 implements Flow {

		@Override
		public void flow(File file) {
			file.close();
		}

	}

	private interface Flow {
		void flow(File file);
	}
	
	@Override
	protected TypestateChangeFunction<ConcreteState> createTypestateChangeFunction() {
		return new FileMustBeClosedStateMachine();
	}
}
