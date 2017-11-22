package typestate.impl.statemachines;

import java.util.Collection;
import java.util.Collections;

import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Unit;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;

public class FileMustBeClosedStateMachine extends TypeStateMachineWeightFunctions{

  public static enum States implements State {
    INIT, OPENED, CLOSED;

    @Override
    public boolean isErrorState() {
      return this == OPENED;
    }

	@Override
	public boolean isInitialState() {
		return false;
	}

	@Override
	public boolean isAccepting() {
		return false;
	}

  }

  public FileMustBeClosedStateMachine() {
    addTransition(new MatcherTransition(States.INIT, ".*open.*",Parameter.This, States.OPENED, Type.OnCall));
    addTransition(new MatcherTransition(States.INIT, ".*close.*",Parameter.This, States.CLOSED, Type.OnCall));
    addTransition(new MatcherTransition(States.OPENED, ".*close.*",Parameter.This, States.CLOSED, Type.OnCall));
  }



  @Override
  public Collection<Val> generateSeed(SootMethod method,Unit unit,
      Collection<SootMethod> calledMethod) {
    try {
		return generateAtAllocationSiteOf(method, unit, Class.forName("typestate.test.helper.File"));
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
	}
    return Collections.emptySet();
  }
}
