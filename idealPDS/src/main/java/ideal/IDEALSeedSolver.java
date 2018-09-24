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
package ideal;

import boomerang.*;
import boomerang.callgraph.ObservableDynamicICFG;
import boomerang.callgraph.ObservableICFG;
import boomerang.debugger.Debugger;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.ForwardBoomerangResults;
import boomerang.seedfactory.SimpleSeedFactory;
import boomerang.solver.AbstractBoomerangSolver;
import com.google.common.base.Stopwatch;
import soot.SootMethod;
import soot.Unit;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;
import wpds.impl.Weight.NoWeight;
import wpds.impl.WeightedPAutomaton;

import java.util.Map.Entry;

public class IDEALSeedSolver<W extends Weight> {

	private final IDEALAnalysisDefinition<W> analysisDefinition;
	private final ForwardQuery seed;
	private final IDEALWeightFunctions<W> idealWeightFunctions;
	private final W zero;
	private final W one;
	private final WeightedBoomerang<W> phase1Solver;
	private final WeightedBoomerang<W> phase2Solver;
	private final Stopwatch analysisStopwatch = Stopwatch.createUnstarted();
	private final SimpleSeedFactory seedFactory;
	private WeightedBoomerang<W> timedoutSolver;
	private Boomerang boomerangSolver;

    public enum Phases {
		ObjectFlow, ValueFlow
	};

	public IDEALSeedSolver(IDEALAnalysisDefinition<W> analysisDefinition, ForwardQuery seed,  SimpleSeedFactory seedFactory) {
		this.analysisDefinition = analysisDefinition;
		this.seed = seed;
		this.seedFactory = seedFactory;
		this.idealWeightFunctions = new IDEALWeightFunctions<>(analysisDefinition.weightFunctions(), analysisDefinition.enableStrongUpdates());
		this.zero = analysisDefinition.weightFunctions().getZero();
		this.one = analysisDefinition.weightFunctions().getOne();
		this.phase1Solver = createSolver();
		this.phase2Solver = createSolver();
		this.boomerangSolver = new Boomerang() {

			@Override
			public ObservableICFG<Unit, SootMethod> icfg() {
				return analysisDefinition.icfg();
			}
		};
	}

	public ForwardBoomerangResults<W> run() {
		ForwardBoomerangResults<W> resultPhase1 = runPhase(this.phase1Solver,Phases.ObjectFlow);
		if(resultPhase1.isTimedout()) {
			if(analysisStopwatch.isRunning()){
				analysisStopwatch.stop();
			}
			timedoutSolver = this.phase1Solver;
			throw new IDEALSeedTimeout(this,this.phase1Solver, resultPhase1);
		}
		ForwardBoomerangResults<W> resultPhase2 = runPhase(this.phase2Solver,Phases.ValueFlow);
		if(resultPhase2.isTimedout()) {
			if(analysisStopwatch.isRunning()){
				analysisStopwatch.stop();
			}
			timedoutSolver = this.phase2Solver;
			throw new IDEALSeedTimeout(this,this.phase2Solver, resultPhase2);
		}
		return resultPhase2;
	}

	private WeightedBoomerang<W> createSolver() {
		return new WeightedBoomerang<W>(analysisDefinition.boomerangOptions()) {
			@Override
			public ObservableICFG<Unit, SootMethod> icfg() {
				if (analysisDefinition.icfg == null){
					analysisDefinition.icfg = new ObservableDynamicICFG<W>(this);
				}
				return analysisDefinition.icfg();
			}

			@Override
			public Debugger<W> createDebugger() {
				return analysisDefinition.debugger(IDEALSeedSolver.this);
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getForwardCallWeights(ForwardQuery sourceQuery) {
				if(sourceQuery.equals(seed))
					return idealWeightFunctions;
				return new OneWeightFunctions<>(zero, one);
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getForwardFieldWeights() {
				return new OneWeightFunctions<>(zero, one);
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getBackwardFieldWeights() {
				return new OneWeightFunctions<>(zero, one);
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getBackwardCallWeights() {
				return new OneWeightFunctions<>(zero, one);
			}
		};
	}

	private ForwardBoomerangResults<W> runPhase(final WeightedBoomerang<W> boomerang, final Phases phase) {
		analysisStopwatch.start();
		idealWeightFunctions.setPhase(phase);
		final WeightedPAutomaton<Statement, INode<Val>, W> callAutomaton = boomerang.getSolvers().getOrCreate(seed).getCallAutomaton();
		callAutomaton.registerConnectPushListener((callSite, returnSite, returnedFact, w) -> {
			if(!callSite.getMethod().equals(returnedFact.fact().m()))
				return;
			if(!callSite.getMethod().equals(returnSite.getMethod()))
				return;
			if(!w.equals(one)){
				idealWeightFunctions.addOtherThanOneWeight(new Node<Statement,Val>(callSite, returnedFact.fact()), w);
			}
		});
		ForwardBoomerangResults<W> res = boomerang.solve(seed);
		idealWeightFunctions.registerListener((curr, weight) -> {
			if(phase.equals(Phases.ValueFlow)){
				return;
			}
			idealWeightFunctions.potentialStrongUpdate(curr.stmt(), weight);
			boomerangSolver.solve(new BackwardQuery(curr.stmt(),curr.fact()));
			if(!res.getAnalysisWatch().isRunning()) {
				res.getAnalysisWatch().start();
			}
			for(final Entry<Query, AbstractBoomerangSolver<NoWeight>> e : boomerangSolver.getSolvers().entrySet()){
				if(e.getKey() instanceof ForwardQuery){
					e.getValue().synchedEmptyStackReachable(curr, targetFact -> {
						if(!e.getKey().asNode().equals(seed.asNode())){
							idealWeightFunctions.weakUpdate(curr.stmt());
						}
					});
				}
			}
		});
		boomerang.debugOutput();
		analysisStopwatch.stop();
		return res;
	}

	public WeightedBoomerang<W> getPhase1Solver() {
		return phase1Solver;
	}

	public WeightedBoomerang<W> getPhase2Solver() {
		return phase2Solver;
	}

	public Stopwatch getAnalysisStopwatch() {
		return analysisStopwatch;
	}

	public boolean isTimedOut(){
		return timedoutSolver != null;
	}

	public WeightedBoomerang getTimedoutSolver() {
		return timedoutSolver;
	}
	public Query getSeed() {
		return seed;
	}
}
