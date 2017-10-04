package test;

import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import boomerang.Boomerang;
import boomerang.ForwardQuery;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import ideal.ResultReporter;
import soot.Unit;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import typestate.TransitionFunction;
import typestate.finiteautomata.State;
import wpds.impl.Transition;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public class TestingResultReporter implements ResultReporter<TransitionFunction>{
	private Multimap<Unit, Assertion> stmtToResults = HashMultimap.create();
	public TestingResultReporter(Set<Assertion> expectedResults) {
		for(Assertion e : expectedResults){
			if(e instanceof ComparableResult)
				stmtToResults.put(((ComparableResult) e).getStmt(), e);
		}
	}

	@Override
	public void onSeedFinished(ForwardQuery seed, AbstractBoomerangSolver<TransitionFunction> seedSolver) {
		for(final Entry<Unit, Assertion> e : stmtToResults.entries()){
			if(e.getValue() instanceof ComparableResult){
				final ComparableResult<TransitionFunction> expectedResults = (ComparableResult) e.getValue();
//				System.out.println(Joiner.on("\n").join(seedSolver.getNodesToWeights().entrySet()));
				for(Entry<Node<Statement, INode<Val>>, TransitionFunction> s : seedSolver.getNodesToWeights().entrySet()){
					Node<Statement, INode<Val>> node = s.getKey();
					if(!node.fact().fact().equals(expectedResults.getVal()))
						continue;
					if(node.stmt().getUnit().isPresent()){
						if(node.stmt().getUnit().get().equals(e.getKey())){
							expectedResults.computedResults(s.getValue());
						}
					}
					
				}
			}
		}
	}


}
