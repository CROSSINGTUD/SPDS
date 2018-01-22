/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package test;

import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import soot.Unit;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAUpdateListener;

public class TestingResultReporter<W extends Weight>{
	private Multimap<Unit, Assertion> stmtToResults = HashMultimap.create();
	public TestingResultReporter(Set<Assertion> expectedResults) {
		for(Assertion e : expectedResults){
			if(e instanceof ComparableResult)
				stmtToResults.put(((ComparableResult) e).getStmt(), e);
		}
	}

	public void onSeedFinished(Node<Statement,Val> seed,final AbstractBoomerangSolver<W> seedSolver) {
		for(final Entry<Unit, Assertion> e : stmtToResults.entries()){
			if(e.getValue() instanceof ComparableResult){
				final ComparableResult<W,Val> expectedResults = (ComparableResult) e.getValue();
				for(Entry<Transition<Statement, INode<Val>>, W> s : seedSolver.getTransitionsToFinalWeights().entrySet()){
					Transition<Statement, INode<Val>> t = s.getKey();
					W w = s.getValue();
					if((t.getStart() instanceof GeneratedState)  || !t.getStart().fact().equals(expectedResults.getVal()))
						continue;
					if(t.getLabel().getUnit().isPresent()){
						if(t.getLabel().getUnit().get().equals(e.getKey())){
							expectedResults.computedResults(w);
						}
					}
				}
			}
		}
	}


}
