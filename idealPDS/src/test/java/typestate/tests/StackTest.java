package typestate.tests;

import java.util.Stack;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.ConcreteState;
import typestate.TypestateChangeFunction;
import typestate.impl.statemachines.VectorStateMachine;

@SuppressWarnings("deprecation")
public class StackTest extends IDEALTestingFramework {

	@Test
	public void test1() {
		Stack s = new Stack();
		if (staticallyUnknown())
			s.peek();
		else {
			Stack r = s;
			r.pop();
			mustBeInErrorState(r);
		}
		mustBeInErrorState(s);
	}

	@Test
	public void test2() {
		Stack s = new Stack();
		s.add(new Object());
		if (staticallyUnknown())
			s.peek();
		else
			s.pop();
		mustBeInAcceptingState(s);
	}

	@Test
	public void test3() {
		Stack s = new Stack();
		s.peek();
		mustBeInErrorState(s);
		s.pop();
		mustBeInErrorState(s);
	}

	@Test
	public void test4() {
		Stack s = new Stack();
		s.peek();
		s.pop();

		Stack c = new Stack();
		c.add(new Object());
		c.peek();
		c.pop();
		mustBeInErrorState(s);
		mustBeInAcceptingState(c);
	}

	@Override
	protected TypestateChangeFunction<ConcreteState> createTypestateChangeFunction() {
		return new VectorStateMachine();
	}
}