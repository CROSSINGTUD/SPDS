package test;

import boomerang.jimple.Val;
import soot.Unit;
import typestate.TransitionFunction;
import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.State;
import typestate.finiteautomata.Transition;

public class MustBe extends ExpectedResults<TransitionFunction> {

	MustBe(Unit unit, Val val, InternalState state) {
		super(unit, val, state);
	}

	public String toString(){
		return "MustBe " + super.toString();
	}

	@Override
	public void computedResults(TransitionFunction val) {
		System.out.println(val + " " + unit + " " + val);
		for(ITransition t : val.values()){
			if(t.equals(Transition.identity())){
				continue;
			}
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
