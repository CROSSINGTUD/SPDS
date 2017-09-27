package typestate.tests;

import org.junit.Test;

import test.IDEALTestingFramework;
import test.slowmethod.SlowMethodDetector;
import typestate.ConcreteState;
import typestate.TypestateChangeFunction;
import typestate.finiteautomata.State;
import typestate.impl.statemachines.FileMustBeClosedStateMachine;
import typestate.test.helper.File;
import typestate.test.helper.ObjectWithField;

@SuppressWarnings("deprecation")
public class FileMustBeClosedTest extends IDEALTestingFramework{
	@Test
	public void simple() {
		File file = new File();
		file.open();
		mustBeInErrorState(file);
		file.close();
		mustBeInAcceptingState(file);
	}

	@Test
	public void simple0() {
		File file = new File();
		file.open();
		escape(file);
		mustBeInErrorState(file);
	}

	private void escape(File other) {
	}

	@Test
	public void simple1() {
		File file = new File();
		File alias = file;
		alias.open();
		mustBeInErrorState(file);
		mustBeInErrorState(alias);
	}

	@Test
	public void branching() {
		File file = new File();
		if (staticallyUnknown())
			file.open();
		mayBeInErrorState(file);
		file.close();
		mustBeInAcceptingState(file);
	}

	@Test
	public void aliasing() {
		File file = new File();
		File alias = file;
		if (staticallyUnknown())
			file.open();
		mayBeInErrorState(file);
		alias.close();
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(alias);
	}

	@Test
	public void summaryTest() {
		File file1 = new File();
		call(file1);
		file1.close();
		mustBeInAcceptingState(file1);
		File file = new File();
		File alias = file;
		call(alias);
		file.close();
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(alias);
	}

	private static void call(File alias) {
		alias.open();
	}

	@Test
	public void interprocedural() {
		File file = new File();
		file.open();
		flows(file, true);
		mayBeInAcceptingState(file);
		mayBeInErrorState(file);
	}

	private static void flows(File file, boolean b) {
		if (b)
			file.close();
	}

	@Test
	public void flowViaField() {
		ObjectWithField container = new ObjectWithField();
		flows(container);
		if (staticallyUnknown())
			container.field.close();

		mayBeInErrorState(container.field);
	}

	private static void flows(ObjectWithField container) {
		container.field = new File();
		File field = container.field;
		field.open();
	}

	@Test
	public void indirectFlow() {
		ObjectWithField a = new ObjectWithField();
		ObjectWithField b = a;
		flows(a, b);
		mayBeInAcceptingState(a.field);
		mayBeInAcceptingState(b.field);
	}

	private void flows(ObjectWithField a, ObjectWithField b) {
		File file = new File();
		file.open();
		a.field = file;
		File alias = b.field;
		mustBeInErrorState(alias);
		alias.close();
	}

	@Test
	public void noStrongUpdatePossible() {
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
		b.close();
		mayBeInErrorState(a);
		mustBeInAcceptingState(b);
	}

	@Test
	public void parameterAlias() {
		File file = new File();
		File alias = file;
		call(alias, file);
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(alias);
	}

	private void call(File file1, File file2) {
		file1.open();
		file2.close();
		mustBeInAcceptingState(file1);
	}

	@Test
	public void parameterAlias2() {
		File file = new File();
		File alias = file;
		call2(alias, file);
		mayBeInErrorState(file);
		mayBeInErrorState(alias);
	}

	private void call2(File file1, File file2) {
		file1.open();
		if (staticallyUnknown())
			file2.close();
	}

	@Test
	public void test() {
		ObjectWithField a = new ObjectWithField();
		ObjectWithField b = a;
		File file = new File();
		file.open();
		bar(a, b, file);
		b.field.close();
		mustBeInAcceptingState(file);
		mustBeInAcceptingState(a.field);
	}

	@Test
	public void noStrongUpdate() {
		ObjectWithField a = new ObjectWithField();
		ObjectWithField b = new ObjectWithField();
		File file = new File();
		if (staticallyUnknown()) {
			b.field = file;
		} else {
			a.field = file;
		}
		a.field.open();
		b.field.close();
		// Debatable
		mayBeInAcceptingState(file);
	}

	@Test
	public void unbalancedReturn1() {
		File second = createOpenedFile();
		mustBeInErrorState(second);
	}

	@Test
	public void unbalancedReturn2() {
		File first = createOpenedFile();
		clse(first);
		mustBeInAcceptingState(first);
		File second = createOpenedFile();
		second.hashCode();
		mustBeInErrorState(second);
	}

	private static void clse(File first) {
		first.close();
	}

	public static File createOpenedFile() {
		File f = new File();
		f.open();
		return f;
	}

	private void bar(ObjectWithField a, ObjectWithField b, File file) {
		a.field = file;
	}

	@Test
	public void lateWriteToField() {
		ObjectWithField a = new ObjectWithField();
		ObjectWithField b = a;
		File file = new File();
		bar(a, file);
		File c = b.field;
		c.close();
		mustBeInAcceptingState(file);
	}

	private void bar(ObjectWithField a, File file) {
		file.open();
		a.field = file;
		mustBeInErrorState(a.field);
	}

	@Test
	public void fieldStoreAndLoad1() {
		ObjectWithField container = new ObjectWithField();
		File file = new File();
		container.field = file;
		File a = container.field;
		a.open();
		mustBeInErrorState(a);
		mustBeInErrorState(file);
	}

	@Test
	public void fieldStoreAndLoad2() {
		ObjectWithField container = new ObjectWithField();
		container.field = new File();
		ObjectWithField otherContainer = new ObjectWithField();
		File a = container.field;
		otherContainer.field = a;
		flowsToField(container);
		mustBeInErrorState(a);
	}

	private void flowsToField(ObjectWithField container) {
		File field = container.field;
		field.open();
	}

	@Test
	public void wrappedClose() {
		File file = new File();
		File alias = file;
		alias.open();
		mustBeInErrorState(alias);
		mustBeInErrorState(file);
		file.wrappedClose();
		mustBeInAcceptingState(alias);
		mustBeInAcceptingState(file);
	}

	@Test
	public void multipleStates() {
		File file = new File();
		file.open();
		mustBeInErrorState(file);
		int x = 1;
		System.out.println(x);
		mustBeInErrorState(file);
		file.close();
		mustBeInAcceptingState(file);
		x = 1;
		System.out.println(x);
		mustBeInAcceptingState(file);
	}

	@Test
	public void doubleBranching() {
		File file = new File();
		if (staticallyUnknown()) {
			file.open();
			if (staticallyUnknown())
				file.close();
		} else if (staticallyUnknown())
			file.close();
		else {
			System.out.println(2);
		}
		mayBeInErrorState(file);
	}
	@Test
	public void whileLoopBranching() {
		File file = new File();
		while(staticallyUnknown()){
			if (staticallyUnknown()) {
				file.open();
				if (staticallyUnknown())
					file.close();
			} else if (staticallyUnknown())
				file.close();
			else {
				System.out.println(2);
			}
		}
		mayBeInErrorState(file);
	}

	@Override
	protected TypestateChangeFunction createTypestateChangeFunction() {
		return new FileMustBeClosedStateMachine();
	}
}
