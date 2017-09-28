package test;

import boomerang.jimple.Val;
import soot.Unit;
import typestate.TransitionFunction;

public interface ComparableResult<State> {

	public Val getVal();
	public Unit getStmt();
	public void computedResults(State val);
}
