package test;

import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import boomerang.ForwardQuery;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import ideal.ResultReporter;
import soot.Unit;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import typestate.TransitionFunction;
import wpds.impl.Transition;
import wpds.impl.WeightedPAutomaton;

public class TestingResultReporter implements ResultReporter<TransitionFunction>{
	private Multimap<Unit, Assertion> stmtToResults = HashMultimap.create();
	public TestingResultReporter(Set<Assertion> expectedResults) {
		for(Assertion e : expectedResults){
			if(e instanceof ComparableResult)
				stmtToResults.put(((ComparableResult) e).getStmt(), e);
		}
	}

	@Override
	public void onSeedFinished(ForwardQuery seed,final AbstractBoomerangSolver<TransitionFunction> seedSolver) {
		for(final Entry<Unit, Assertion> e : stmtToResults.entries()){
			if(e.getValue() instanceof ComparableResult){
				final ComparableResult<TransitionFunction> expectedResults = (ComparableResult) e.getValue();
//				System.out.println(Joiner.on("\n").join(seedSolver.getNodesToWeights().entrySet()));
				WeightedPAutomaton<Statement, INode<Val>, TransitionFunction> aut = new WeightedPAutomaton<Statement, INode<Val>, TransitionFunction>(null) {
					@Override
					public INode<Val> createState(INode<Val> d, Statement loc) {
						return null;
					}

					@Override
					public boolean isGeneratedState(INode<Val> d) {
						return false;
					}

					@Override
					public Statement epsilon() {
						return seedSolver.getCallAutomaton().epsilon();
					}

					@Override
					public TransitionFunction getZero() {
						return seedSolver.getCallAutomaton().getZero();
					}

					@Override
					public TransitionFunction getOne() {
						return seedSolver.getCallAutomaton().getOne();
					}
				};
				
//				for(Entry<Transition<Statement, INode<Val>>, TransitionFunction> s : seedSolver.getTransitionsToFinalWeights().entrySet()){
//					aut.addWeightForTransition(s.getKey(), s.getValue());
//					Transition<Statement, INode<Val>> node = s.getKey();
//					if((node.getStart() instanceof GeneratedState)  || !node.getStart().fact().equals(expectedResults.getVal()))
//						continue;
//					if(node.getLabel().getUnit().isPresent()){
//						if(node.getLabel().getUnit().get().equals(e.getKey())){
//							expectedResults.computedResults(s.getValue());
//						}
//					}
//				}
				for(Transition<Statement, INode<Val>> node : seedSolver.getCallAutomaton().getTransitions()){
					if((node.getStart() instanceof GeneratedState)  || !node.getStart().fact().equals(expectedResults.getVal()))
						continue;
					if(node.getLabel().getUnit().isPresent()){
						if(node.getLabel().getUnit().get().equals(e.getKey())){
							expectedResults.computedResults(seedSolver.getCallAutomaton().getWeightFor(node));
						}
					}
				}
				System.out.println("FINAL WEIGHT AUTOMATON");
				System.out.println(aut.toDotString());
			}
		}
	}


}
