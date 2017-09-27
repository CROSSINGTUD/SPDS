package typestate.tests;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.ConcreteState;
import typestate.TypestateChangeFunction;
import typestate.impl.statemachines.FileMustBeClosedStateMachine;
import typestate.test.helper.File;
import typestate.test.helper.ObjectWithField;

@SuppressWarnings("deprecation")
public class FileMustBeClosedNullPOATest extends IDEALTestingFramework {

	@Test
	public void nullPOATest1() {
		ObjectWithField container = new ObjectWithField();
		flows(container);
		if (container.field != null)
			container.field.close();
		mustBeInAcceptingState(container.field);
	}

	private static void flows(ObjectWithField container) {
		container.field = new File();
		File field = container.field;
		field.open();
	}

	@Test
	public void nullPOATest2() {
		File file = null;
		if (staticallyUnknown())
			file = new File();

		file.open();
		if (file != null)
			file.close();
		mustBeInAcceptingState(file);
	}

	@Test
	public void nullPOATest3() {
		ObjectWithField a = new ObjectWithField();
		ObjectWithField b = a;
		File file = new File();
		file.open();
		flows(a, b, file);
		mustBeInAcceptingState(a.field);
		mustBeInAcceptingState(b.field);
	}

	@Test
	public void nullPOATest4() {
		ObjectWithField a = new ObjectWithField();
		ObjectWithField b = a;
		File file = new File();
		a.field = file;
		file.open();
		if (b.field != null)
			b.field.close();
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(a.field);
	}

	private static void flows(ObjectWithField a, ObjectWithField b, File file) {
		a.field = file;
		File alias = b.field;
		if (alias != null)
			alias.close();
	}

	@Test
	public void nullPOATest5() {
		File b = null;
		File a = new File();
		a.open();
		File e = new File();
		e.open();
		if (staticallyUnknown()) {
			b = a;
		} else {
			b = e;
		}
		if (b != null)
			b.close();
		mayBeInAcceptingState(b);
		mayBeInAcceptingState(a);
	}

	@Override
	protected TypestateChangeFunction<ConcreteState> createTypestateChangeFunction() {
		return new FileMustBeClosedStateMachine();
	}
}
