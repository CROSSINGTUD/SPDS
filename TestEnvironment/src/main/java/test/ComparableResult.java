package test;

import soot.Unit;

public interface ComparableResult<State, Val> {

	public Val getVal();
	public Unit getStmt();
	public void computedResults(State val);
}
