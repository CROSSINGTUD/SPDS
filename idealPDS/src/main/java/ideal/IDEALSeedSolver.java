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

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Stopwatch;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.debugger.Debugger;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.poi.BaseSolverContext;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.seedfactory.SeedFactory;
import boomerang.solver.AbstractBoomerangSolver;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.EmptyStackWitnessListener;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.ConnectPushListener;
import wpds.impl.PAutomaton;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public class IDEALSeedSolver<W extends Weight> {

	private final IDEALAnalysisDefinition<W> analysisDefinition;
	private final ForwardQuery seed;
	private final IDEALWeightFunctions<W> idealWeightFunctions;
	private final W zero;
	private final W one;
	private final WeightedBoomerang<W> phase1Solver;
	private final WeightedBoomerang<W> phase2Solver;
	private final Stopwatch analysisStopwatch = Stopwatch.createUnstarted();
	private final SeedFactory<W> seedFactory;
	private WeightedBoomerang<W> timedoutSolver;

	private final class ImportFieldAut implements WPAUpdateListener<Field, INode<Node<Statement, Val>>, W> {
		private final Node<Statement, Val> curr;
		private WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> importTo;
		private WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> importFrom;
		private AbstractBoomerangSolver<W> forwardSolver;

		private ImportFieldAut(AbstractBoomerangSolver<W> forwardSolver, Node<Statement, Val> curr,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> importFrom,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> importTo) {
			this.forwardSolver = forwardSolver;
			this.curr = curr;
			this.importFrom = importFrom;
			this.importTo = importTo;
		}

		@Override
		public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
			if (t.getStart().fact().equals(curr)) {
				if (t.getLabel().equals(Field.empty())) {
					System.out.println("SOLVING");
					System.out.println(new Transition<Field, INode<Node<Statement, Val>>>(t.getStart(),
							t.getLabel(), importTo.getInitialState()));
					for(Statement pred : forwardSolver.getPredsOf(t.getStart().fact().stmt())) {
						importTo.addTransition(new Transition<Field, INode<Node<Statement, Val>>>(new SingleNode<Node<Statement,Val>>(new Node<>(pred,t.getStart().fact().fact())),
							t.getLabel(), importTo.getInitialState()));
					}
				} else {
					System.out.println("SOLVINGB");
					System.out.println(t);
					for(Statement pred : forwardSolver.getPredsOf(t.getStart().fact().stmt())) {
						importTo.addTransition(new Transition<Field, INode<Node<Statement, Val>>>(new SingleNode<Node<Statement,Val>>(new Node<>(pred,t.getStart().fact().fact())),
							t.getLabel(), t.getTarget()));
					}
					importFrom.registerListener(new ImportFieldAutTrans(t.getTarget(), importFrom, importTo));
				}
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((curr == null) ? 0 : curr.hashCode());
			result = prime * result + ((importFrom == null) ? 0 : importFrom.hashCode());
			result = prime * result + ((importTo == null) ? 0 : importTo.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ImportFieldAut other = (ImportFieldAut) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (curr == null) {
				if (other.curr != null)
					return false;
			} else if (!curr.equals(other.curr))
				return false;
			if (importFrom == null) {
				if (other.importFrom != null)
					return false;
			} else if (!importFrom.equals(other.importFrom))
				return false;
			if (importTo == null) {
				if (other.importTo != null)
					return false;
			} else if (!importTo.equals(other.importTo))
				return false;
			return true;
		}

		private IDEALSeedSolver getOuterType() {
			return IDEALSeedSolver.this;
		}

	}

	private final class ImportFieldAutTrans extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {
		private WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> importFrom;
		private WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> importTo;

		public ImportFieldAutTrans(INode<Node<Statement, Val>> target,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> importFrom,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> importTo) {
			super(target);
			this.importFrom = importFrom;
			this.importTo = importTo;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if (t.getLabel().equals(Field.empty())) {

				System.out.println("SOLVINGB");
				System.out.println(new Transition<Field, INode<Node<Statement, Val>>>(t.getStart(), t.getLabel(),
						importTo.getInitialState()));
				importTo.addTransition(new Transition<Field, INode<Node<Statement, Val>>>(t.getStart(), t.getLabel(),
						importTo.getInitialState()));
			} else {

				System.out.println("SOLVINGBD");
				System.out.println(t);
				importTo.addTransition(t);
				importFrom.registerListener(new ImportFieldAutTrans(t.getTarget(), importFrom, importTo));
			}
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((importFrom == null) ? 0 : importFrom.hashCode());
			result = prime * result + ((importTo == null) ? 0 : importTo.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			ImportFieldAutTrans other = (ImportFieldAutTrans) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (importFrom == null) {
				if (other.importFrom != null)
					return false;
			} else if (!importFrom.equals(other.importFrom))
				return false;
			if (importTo == null) {
				if (other.importTo != null)
					return false;
			} else if (!importTo.equals(other.importTo))
				return false;
			return true;
		}

		private IDEALSeedSolver getOuterType() {
			return IDEALSeedSolver.this;
		}

	}

	public enum Phases {
		ObjectFlow, ValueFlow
	};

	public IDEALSeedSolver(IDEALAnalysisDefinition<W> analysisDefinition, ForwardQuery seed,
			SeedFactory<W> seedFactory) {
		this.analysisDefinition = analysisDefinition;
		this.seed = seed;
		this.seedFactory = seedFactory;
		this.idealWeightFunctions = new IDEALWeightFunctions<W>(analysisDefinition.weightFunctions(),
				analysisDefinition.enableStrongUpdates());
		this.zero = analysisDefinition.weightFunctions().getZero();
		this.one = analysisDefinition.weightFunctions().getOne();
		this.phase1Solver = createSolver(Phases.ObjectFlow);
		this.phase2Solver = createSolver(Phases.ValueFlow);
	}

	public ForwardBoomerangResults<W> run() {
		ForwardBoomerangResults<W> resultPhase1 = runPhase(this.phase1Solver, Phases.ObjectFlow);
		if (resultPhase1.isTimedout()) {
			if (analysisStopwatch.isRunning()) {
				analysisStopwatch.stop();
			}
			timedoutSolver = this.phase1Solver;
			throw new IDEALSeedTimeout(this, this.phase1Solver, resultPhase1);
		}
		idealWeightFunctions.printStrongUpdates();
		ForwardBoomerangResults<W> resultPhase2 = runPhase(this.phase2Solver, Phases.ValueFlow);
		if (resultPhase2.isTimedout()) {
			if (analysisStopwatch.isRunning()) {
				analysisStopwatch.stop();
			}
			timedoutSolver = this.phase2Solver;
			throw new IDEALSeedTimeout(this, this.phase2Solver, resultPhase2);
		}
		return resultPhase2;
	}

	private WeightedBoomerang<W> createSolver(Phases phase) {
		return new WeightedBoomerang<W>(analysisDefinition.boomerangOptions()) {
			@Override
			public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
				return analysisDefinition.icfg();
			}

			@Override
			public Debugger<W> createDebugger() {
				return analysisDefinition.debugger(IDEALSeedSolver.this);
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getForwardCallWeights(ForwardQuery sourceQuery) {
				if (sourceQuery.equals(seed))
					return idealWeightFunctions;
				return new OneWeightFunctions<Statement, Val, Statement, W>(zero, one);
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getForwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, W>(zero, one);
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getBackwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, W>(zero, one);
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getBackwardCallWeights() {
				return new OneWeightFunctions<Statement, Val, Statement, W>(zero, one);
			}

			@Override
			public SeedFactory<W> getSeedFactory() {
				return seedFactory;
			}

			@Override
			public boolean preventForwardCallTransitionAdd(ForwardQuery sourceQuery,
					Transition<Statement, INode<Val>> t, W weight) {
				if (phase.equals(Phases.ValueFlow) && sourceQuery.equals(seed)) {
					if (preventStrongUpdateFlows(t, weight)) {
						return true;
					}
				}
				return super.preventForwardCallTransitionAdd(sourceQuery, t, weight);
			}
		};
	}

	protected boolean preventStrongUpdateFlows(Transition<Statement, INode<Val>> t, W weight) {
		if (idealWeightFunctions.isStrongUpdateStatement(t.getLabel())) {
			if (!idealWeightFunctions
					.containsIndirectFlow(new Node<Statement, Val>(t.getLabel(), t.getStart().fact()))) {
				if ((t.getStart() instanceof GeneratedState)) {
				} else {
					System.out.println("PREVENT ADDING " + t);
					return true;
				}
			}
		}
		return false;
	}

	private ForwardBoomerangResults<W> runPhase(final WeightedBoomerang<W> boomerang, final Phases phase) {
		analysisStopwatch.start();
		idealWeightFunctions.setPhase(phase);
		final WeightedPAutomaton<Statement, INode<Val>, W> callAutomaton = boomerang.getSolvers().getOrCreate(seed)
				.getCallAutomaton();
		callAutomaton.registerConnectPushListener(new ConnectPushListener<Statement, INode<Val>, W>() {

			@Override
			public void connect(Statement callSite, Statement returnSite, INode<Val> returnedFact, W w) {
				if (!callSite.getMethod().equals(returnedFact.fact().m()))
					return;
				if (!callSite.getMethod().equals(returnSite.getMethod()))
					return;
				if (!w.equals(one)) {
					System.out.println("RECONNECT " + callSite);
					idealWeightFunctions.addOtherThanOneWeight(new Node<Statement, Val>(callSite, returnedFact.fact()),
							w);
				}
			}
		});

		if (phase.equals(Phases.ValueFlow)) {
			registerIndirectFlowListener(boomerang.getSolvers().getOrCreate(seed));
		}
		ForwardBoomerangResults<W> res = boomerang.solve((ForwardQuery) seed);
		idealWeightFunctions.registerListener(new NonOneFlowListener<W>() {
			@Override
			public void nonOneFlow(final Node<Statement, Val> curr, final W weight) {
				if (phase.equals(Phases.ValueFlow)) {
					return;
				}
				idealWeightFunctions.potentialStrongUpdate(curr.stmt());
				WeightedBoomerang<W>.AccessPathBackwardQuery query = boomerang.new AccessPathBackwardQuery(curr.stmt(),
						curr.fact());
				boomerang.getSolvers().get(seed).getFieldAutomaton().registerListener(new ImportFieldAut(boomerang.getSolvers().get(seed),curr,
						boomerang.getSolvers().get(seed).getFieldAutomaton(), query.getFieldAutomaton()));
				BackwardBoomerangResults<W> backwardSolveUnderScope = boomerang.backwardSolveUnderScope(query, seed,
						curr);
				if (!res.getAnalysisWatch().isRunning()) {
					res.getAnalysisWatch().start();
				}
				System.out.println("NON ONE FLOW  " + curr + weight);

				for (final Entry<Query, AbstractBoomerangSolver<W>> e : boomerang.getSolvers().entrySet()) {
					if (e.getKey() instanceof ForwardQuery) {
						e.getValue().synchedEmptyStackReachable(curr, new EmptyStackWitnessListener<Statement, Val>() {
							@Override
							public void witnessFound(Node<Statement, Val> targetFact) {
								if (!e.getKey().asNode().equals(seed.asNode())) {
									idealWeightFunctions.weakUpdate(curr.stmt());
								}
							}
						});
					}
				}

				Map<ForwardQuery, PAutomaton<Statement, INode<Val>>> allocationSites = backwardSolveUnderScope
						.getAllocationSites();
				for (ForwardQuery e : allocationSites.keySet()) {
					AbstractBoomerangSolver<W> solver = boomerang.getSolvers().get(e);
					System.out.println("ALLOC " + e);
					solver.getCallAutomaton().registerListener(new WPAUpdateListener<Statement, INode<Val>, W>() {
						@Override
						public void onWeightAdded(Transition<Statement, INode<Val>> t, W w,
								WeightedPAutomaton<Statement, INode<Val>, W> aut) {
							for (Statement succ : solver.getSuccsOf(curr.stmt())) {
								if (t.getLabel().equals(succ) && !t.getStart().fact().equals(curr.fact())) {
									idealWeightFunctions.addNonKillFlow(curr);
									System.out
											.println("INDIRECT ALIASES " + new Node<Statement, Val>(succ, curr.fact()));
									idealWeightFunctions.addIndirectFlow(new Node<Statement, Val>(succ, curr.fact()),
											new Node<Statement, Val>(succ, t.getStart().fact()));
								}
							}
						}
					});
				}
			}
		});
		boomerang.debugOutput();
		analysisStopwatch.stop();
		return res;
	}

	private void registerIndirectFlowListener(AbstractBoomerangSolver<W> solver) {
		WeightedPAutomaton<Statement, INode<Val>, W> callAutomaton = solver.getCallAutomaton();
		callAutomaton.registerListener(new WPAUpdateListener<Statement, INode<Val>, W>() {

			@Override
			public void onWeightAdded(Transition<Statement, INode<Val>> t, W w,
					WeightedPAutomaton<Statement, INode<Val>, W> aut) {
				if (t.getStart() instanceof GeneratedState)
					return;
				Node<Statement, Val> source = new Node<Statement, Val>(t.getLabel(), t.getStart().fact());
				Collection<Node<Statement, Val>> indirectFlows = idealWeightFunctions.getAliasesFor(source);
				if (!indirectFlows.isEmpty())
					System.out.println("GET INDIRECT ALIASES " + source);
				for (Node<Statement, Val> indirect : indirectFlows) {
					System.out.println("ADDING " + new Transition<Statement, INode<Val>>(
							new SingleNode<>(indirect.fact()), t.getLabel(), t.getTarget()));
					// callAutomaton.addWeightForTransition(new Transition<Statement,
					// INode<Val>>(new SingleNode<>(indirect.fact()), t.getLabel(),
					// t.getTarget()),w);
					solver.addNormalCallFlow(source, indirect);
					for (Statement pred : solver.getPredsOf(t.getLabel())) {
						solver.addNormalFieldFlow(new Node<Statement, Val>(pred, indirect.fact()), indirect);
					}

				}
			}
		});
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

	public boolean isTimedOut() {
		return timedoutSolver != null;
	}

	public WeightedBoomerang getTimedoutSolver() {
		return timedoutSolver;
	}

	public Query getSeed() {
		return seed;
	}
}
