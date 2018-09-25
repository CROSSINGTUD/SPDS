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
package test.cases.fields;

import org.junit.Ignore;
import org.junit.Test;

import boomerang.SolverCreationListener;
import boomerang.WeightedBoomerang;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import test.core.AbstractBoomerangTest;
import wpds.impl.Transition;
import wpds.impl.Weight.NoWeight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAUpdateListener;

public class HiddenFieldLoadAdvancedTest extends AbstractBoomerangTest{
	@Test
	@Ignore
	public void run(){
		A b = new A();
		A a = b;
		b.setF();
		int x = 1;
		Object alias = a.f();
		queryFor(alias);
	}
	
	@Override
	protected void setupSolver(WeightedBoomerang<NoWeight> solver) {
		doesNotContainTransition(solver,"queryFor(alias)", "x = 1", "b", "{}");
		doesNotContainTransition(solver,"queryFor(alias)", "a.f(", "b", "{}");
	}
	
	

	private void doesNotContainTransition(WeightedBoomerang<NoWeight> solver, String solverMatch, String stmt, String fact, String fieldlabel) {
		solver.registerSolverCreationListener(new SolverCreationListener<NoWeight>() {
			
			@Override
			public void onCreatedSolver(AbstractBoomerangSolver<NoWeight> solver) {
				if(solver instanceof BackwardBoomerangSolver) {
					BackwardBoomerangSolver<NoWeight> bsolver = (BackwardBoomerangSolver) solver;
					if(bsolver.getFieldAutomaton().getInitialState().toString().contains(solverMatch)){
						bsolver.getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, NoWeight>() {

							@Override
							public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, NoWeight w,
									WeightedPAutomaton<Field, INode<Node<Statement, Val>>, NoWeight> aut) {
								if(t.getStart().fact().stmt().toString().contains(stmt) && t.getStart().fact().fact().toString().contains(fact) && t.getLabel().toString().contains(fieldlabel))  {
									throw new RuntimeException("Did not except this transition " + t);
								}
							}

						});
					}
					
				}
			}
		});
		
	}




	private static class A{
		Object f;
		public void setF() {
			f = new Alloc();
		}

		public void setF(Object alloc) {
			f = alloc;
		}
		public Object f() {
			return f;
		}
		
	}
}
