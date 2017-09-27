package typestate.tests;

import java.util.Vector;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.ConcreteState;
import typestate.TypestateChangeFunction;
import typestate.impl.statemachines.VectorStateMachine;

@SuppressWarnings("deprecation")
public class VectorTest extends IDEALTestingFramework {

	@Test
	public void test1() {
		Vector s = new Vector();
		s.lastElement();
		mustBeInErrorState(s);
	}

	@Test
	public void test2() {
		Vector s = new Vector();
		s.add(new Object());
		s.firstElement();
		mustBeInAcceptingState(s);
	}

	@Test
	public void test3() {
		Vector v = new Vector();
		try {
			v.removeAllElements();
			v.firstElement();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mustBeInErrorState(v);
	}

	@Test
	public void test4() {
		Vector v = new Vector();
		v.add(new Object());
		try {
			v.firstElement();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mustBeInAcceptingState(v);
		if (staticallyUnknown()) {
			v.removeAllElements();
			v.firstElement();
			mustBeInErrorState(v);
		}
		mayBeInErrorState(v);
	}

	@Test
	public void test6() {
		Vector v = new Vector();
		v.add(new Object());
		mustBeInAcceptingState(v);
		if (staticallyUnknown()) {
			v.removeAllElements();
			v.firstElement();
			mustBeInErrorState(v);
		}
		mayBeInErrorState(v);
	}

	@Test
	public void test5() {
		Vector s = new Vector();
		s.add(new Object());
		if (staticallyUnknown())
			s.firstElement();
		else
			s.elementAt(0);
		mustBeInAcceptingState(s);
	}
	
	@Override
	protected TypestateChangeFunction<ConcreteState> createTypestateChangeFunction() {
		return new VectorStateMachine();
	}
}