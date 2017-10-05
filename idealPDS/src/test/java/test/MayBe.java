package test;

import boomerang.jimple.Val;
import soot.Unit;
import typestate.TransitionFunction;
import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.State;
import typestate.finiteautomata.Transition;

public class MayBe extends ExpectedResults<TransitionFunction> {

	MayBe(Unit unit, Val accessGraph, InternalState state) {
		super(unit, accessGraph, state);
	}
	public String toString(){
		return "Maybe " + super.toString();
	}
	@Override
	public void computedResults(TransitionFunction results) {
		System.out.println(this  + " " +results);
		for(ITransition t : results.values()){
			if(t.equals(Transition.identity()))
				continue;
			State s = t.to();
			if(state == InternalState.ACCEPTING){
				satisfied |= !s.isErrorState();
			} else if(state == InternalState.ERROR){
				satisfied |= s.isErrorState();
			}
		}
	}
}