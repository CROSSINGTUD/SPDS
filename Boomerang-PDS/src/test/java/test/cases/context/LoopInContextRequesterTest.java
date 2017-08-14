package test.cases.context;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class LoopInContextRequesterTest extends AbstractBoomerangTest{

  @Test
  public void loop() {
    ILoop c;
    c = new Loop1();
    c.loop();
  }
	public interface ILoop {
		void loop();
	}

	public class Loop1 implements ILoop {
		A a = new A();
		@Override
		public void loop() {
			if(staticallyUnknown())
				loop();
			AllocatedObject x = a.d;
			queryFor(x);
		}

	}

	public class A{
		AllocatedObject d = new AllocatedObject() {
		};
		A f = new A();
	}
}
