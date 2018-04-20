/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package test;

import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.ForwardBoomerangResults;
import soot.Unit;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;


public class TestingResultReporter<W extends Weight>{
	private Multimap<Unit, Assertion> stmtToResults = HashMultimap.create();
	public TestingResultReporter(Set<Assertion> expectedResults) {
		for(Assertion e : expectedResults){
			if(e instanceof ComparableResult)
				stmtToResults.put(((ComparableResult) e).getStmt(), e);
		}
	}

	public void onSeedFinished(Node<Statement,Val> seed,final ForwardBoomerangResults<W> res) {
		Table<Statement, Val, W> results = res.getResults();
		for(final Entry<Unit, Assertion> e : stmtToResults.entries()){
			if(e.getValue() instanceof ComparableResult){
				final ComparableResult<W,Val> expectedResults = (ComparableResult) e.getValue();
				W w2 = results.get(new Statement((Stmt)e.getKey(), null), expectedResults.getVal());
				if(w2 != null) {
					expectedResults.computedResults(w2);
				}
			}
			//check if any of the methods that should not be analyzed have been analyzed
			if (e.getValue() instanceof ShouldNotBeAnalyzed){
				final ShouldNotBeAnalyzed shouldNotBeAnalyzed = (ShouldNotBeAnalyzed) e.getValue();
				//TODO check if you can do this without seedsolver or get ib ack
				for(Entry<Transition<Statement, INode<Val>>, W> s : seedSolver.getTransitionsToFinalWeights().entrySet()){
					Transition<Statement, INode<Val>> t = s.getKey();
					W w = s.getValue();
					if((t.getStart() instanceof GeneratedState))
						continue;
					if(t.getLabel().getUnit().isPresent()) {
						//check whether we have an assertion here
						if(t.getLabel().getUnit().get().equals(e.getKey())){
							//We have included this in analysis
							shouldNotBeAnalyzed.hasBeenAnalyzed();
						}
					}
				}
			}
		}
	}


}
