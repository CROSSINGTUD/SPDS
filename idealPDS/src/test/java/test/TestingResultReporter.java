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
import boomerang.solver.AbstractBoomerangSolver;
import soot.Unit;
import soot.jimple.Stmt;
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

	public void onSeedFinished(Node<Statement,Val> seed,final ForwardBoomerangResults<W> res) {
		Table<Statement, Val, W> results = res.asStatementValWeightTable();
		for(final Entry<Unit, Assertion> e : stmtToResults.entries()){
			if(e.getValue() instanceof ComparableResult){
				final ComparableResult<W,Val> expectedResults = (ComparableResult) e.getValue();
				W w2 = results.get(new Statement((Stmt)e.getKey(), null), expectedResults.getVal());
				if(w2 != null) {
					expectedResults.computedResults(w2);
				}
			}
		}
	}


}
