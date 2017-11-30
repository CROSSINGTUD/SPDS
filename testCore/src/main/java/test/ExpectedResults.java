package test;

import soot.Unit;

public abstract class ExpectedResults<State, Val> implements Assertion, ComparableResult<State,Val>{
	final Unit unit;
	final Val val;
	final InternalState state;
	protected boolean satisfied;
	protected boolean imprecise;

	enum InternalState{
		ERROR, ACCEPTING;
	}
	ExpectedResults(Unit unit, Val val, InternalState state){
		this.unit = unit;
		this.val = val;
		this.state = state;
	}
	public boolean isSatisfied(){
		return satisfied;
	}
	
	public boolean isImprecise(){
		return imprecise;
	}

	public Val getVal() {
		return val;
	}
	public Unit getStmt() {
		return unit;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((val == null) ? 0 : val.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpectedResults other = (ExpectedResults) obj;
		if (val == null) {
			if (other.val != null)
				return false;
		} else if (!val.equals(other.val))
			return false;
		if (state != other.state)
			return false;
		if (unit == null) {
			if (other.unit != null)
				return false;
		} else if (!unit.equals(other.unit))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "[" + val +" @ " + unit + " in state " + state + "]";
	}
	
}
