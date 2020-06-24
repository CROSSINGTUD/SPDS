package typestate.tests;

import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.FileMustBeClosedStateMachine;
import typestate.test.helper.File;

public class FileMustBeClosedTestDemandDriven extends IDEALTestingFramework {
  @Test
  public void notCaughtByCHA() {
    I b = new B();
    callOnInterface(b);
  }

  private void callOnInterface(I i) {
    File file = new File();
    file.open();
    i.flow(file);
    mustBeInAcceptingState(file);
  }

  @Test
  public void notCaughtByRTA() {
    I a = new A();
    I b = new B();
    callOnInterface(b);
  }

  private interface I {
    void flow(File f);
  }

  private static class B implements I {
    @Override
    public void flow(File f) {
      f.close();
    }
  }

  private static class A implements I {
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
