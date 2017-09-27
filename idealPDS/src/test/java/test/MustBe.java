package test;

import boomerang.jimple.Val;
import soot.Unit;
import typestate.TransitionFunction;
import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.State;

public class MustBe extends ExpectedResults<TransitionFunction> {

	MustBe(Unit unit, Val accessGraph, InternalState state) {
		super(unit, accessGraph, state);
	}

	public String toString(){
		return "MustBe " + super.toString();
	}

	@Override
	public void computedResults(TransitionFunction val) {
		for(ITransition t : val.values()){
			State s = t.to();
			if(state == InternalState.ACCEPTING){
				satisfied |= !s.isErrorState();
				imprecise = val.values().size() > 1;
			} else if(state == InternalState.ERROR){
				satisfied |= s.isErrorState();
				imprecise = val.values().size() > 1;
			}
		}
	}

}	
