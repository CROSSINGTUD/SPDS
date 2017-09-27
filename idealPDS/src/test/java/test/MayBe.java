package test;

import boomerang.jimple.Val;
import soot.Unit;
import typestate.ConcreteState;
import typestate.TypestateDomainValue;

public class MayBe extends ExpectedResults<ConcreteState> {

	MayBe(Unit unit, Val accessGraph, InternalState state) {
		super(unit, accessGraph, state);
	}
	public String toString(){
		return "Maybe " + super.toString();
	}
	@Override
	public void computedResults(TypestateDomainValue<ConcreteState> results) {
		for(ConcreteState s : results.getStates()){
			if(state == InternalState.ACCEPTING){
				satisfied |= !s.isErrorState();
			} else if(state == InternalState.ERROR){
				satisfied |= s.isErrorState();
			}
		}
	}
}	
