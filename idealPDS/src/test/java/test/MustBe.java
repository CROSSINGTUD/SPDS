package test;

import boomerang.jimple.Val;
import soot.Unit;
import typestate.TransitionFunction;
import typestate.finiteautomata.State;

public class MustBe extends ExpectedResults<State> {

	MustBe(Unit unit, Val accessGraph, InternalState state) {
		super(unit, accessGraph, state);
	}

	public String toString(){
		return "MustBe " + super.toString();
	}

	@Override
	public void computedResults(TransitionFunction<State> val) {
		// TODO Auto-generated method stub
		
	}


//	public void computedResults(TypestateDomainValue<ConcreteState> results) {
//		for(ConcreteState s : results.getStates()){
//			if(state == InternalState.ACCEPTING){
//				satisfied |= !s.isErrorState();
//				imprecise = results.getStates().size() > 1;
//			} else if(state == InternalState.ERROR){
//				satisfied |= s.isErrorState();
//				imprecise = results.getStates().size() > 1;
//			}
//		}
//		
//	}
}	
