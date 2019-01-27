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
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.callgraph.ObservableDynamicICFG;
import boomerang.callgraph.ObservableICFG;
import boomerang.callgraph.ObservableStaticICFG;
import boomerang.debugger.Debugger;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.seedfactory.SeedFactory;
import boomerang.solver.AbstractBoomerangSolver;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sync.pds.solver.EmptyStackWitnessListener;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.ConnectPushListener;
import wpds.impl.NormalRule;
import wpds.impl.PushRule;
import wpds.impl.Rule;
import wpds.impl.StackListener;
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
	private Multimap<Node<Statement, Val>, Statement> affectedStrongUpdateStmt = HashMultimap.create();
	private Set<Node<Statement, Val>> weakUpdates = Sets.newHashSet();
	private final class AddIndirectFlowAtCallSite implements WPAUpdateListener<Statement, INode<Val>, W> {
		private final Statement callSite;
		private final Val returnedFact;

		private AddIndirectFlowAtCallSite(Statement callSite, Val returnedFact) {
			this.callSite = callSite;
			this.returnedFact = returnedFact;
		}

		@Override
		public void onWeightAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> aut) {
			if (t.getLabel().equals(callSite)) {
				idealWeightFunctions.addNonKillFlow(new Node<Statement, Val>(callSite, returnedFact));
				idealWeightFunctions.addIndirectFlow(
						new Node<Statement, Val>(callSite, returnedFact),
						new Node<Statement, Val>(callSite, t.getStart().fact()));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
			result = prime * result + ((returnedFact == null) ? 0 : returnedFact.hashCode());
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
			AddIndirectFlowAtCallSite other = (AddIndirectFlowAtCallSite) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (callSite == null) {
				if (other.callSite != null)
					return false;
			} else if (!callSite.equals(other.callSite))
				return false;
			if (returnedFact == null) {
				if (other.returnedFact != null)
					return false;
			} else if (!returnedFact.equals(other.returnedFact))
				return false;
			return true;
		}

		private IDEALSeedSolver getOuterType() {
			return IDEALSeedSolver.this;
		}
		
	}
	private final class IndirectFlowsAtCallSite implements ConnectPushListener<Statement, INode<Val>, W> {


		private final AbstractBoomerangSolver<W> solver;
		private final Statement cs;

		private IndirectFlowsAtCallSite(AbstractBoomerangSolver<W> solver, Statement cs) {
			this.solver = solver;
			this.cs = cs;
		}

		@Override
		public void connect(Statement predOfCall, Statement callSite, INode<Val> returnedFact, W w) {
			if(!solver.valueUsedInStatement((Stmt) callSite.getUnit().get(), returnedFact.fact())) {
				return;
			}
			if (callSite.equals(cs)) {
				solver.getCallAutomaton().registerListener(new AddIndirectFlowAtCallSite(callSite, returnedFact.fact()));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((cs == null) ? 0 : cs.hashCode());
			result = prime * result + ((solver == null) ? 0 : solver.hashCode());
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
			IndirectFlowsAtCallSite other = (IndirectFlowsAtCallSite) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (cs == null) {
				if (other.cs != null)
					return false;
			} else if (!cs.equals(other.cs))
				return false;
			if (solver == null) {
				if (other.solver != null)
					return false;
			} else if (!solver.equals(other.solver))
				return false;
			return true;
		}

		private IDEALSeedSolver getOuterType() {
			return IDEALSeedSolver.this;
		}
	}

	private final class TriggerBackwardQuery extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

		private final AbstractBoomerangSolver<W> seedSolver;
		private final WeightedBoomerang<W> boomerang;
		private final Node<Statement, Val> strongUpdateNode;

		private TriggerBackwardQuery(AbstractBoomerangSolver<W> seedSolver, WeightedBoomerang<W> boomerang, Node<Statement, Val> curr) {
			super(new SingleNode<Node<Statement, Val>>(curr));
			this.seedSolver = seedSolver;
			this.boomerang = boomerang;
			this.strongUpdateNode = curr;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if (!t.getLabel().equals(Field.empty())) {
				return;
			}

			addAffectedPotentialStrongUpdate(strongUpdateNode, strongUpdateNode.stmt());
			for(Unit u : analysisDefinition.icfg().getPredsOf(strongUpdateNode.stmt().getUnit().get())){
				BackwardQuery query = new BackwardQuery(new Statement((Stmt)u, strongUpdateNode.stmt().getMethod()), strongUpdateNode.fact());
				BackwardBoomerangResults<W> queryResults = boomerang.backwardSolveUnderScope(query, seed, strongUpdateNode);
				
				Set<ForwardQuery> queryAllocationSites = queryResults
						.getAllocationSites().keySet();
				setWeakUpdateIfNecessary();
				injectAliasesAtStrongUpdates(queryAllocationSites);
				injectAliasesAtStrongUpdatesAtCallStack(queryAllocationSites);
			}
		}

		private void injectAliasesAtStrongUpdatesAtCallStack(Set<ForwardQuery> queryAllocationSites) {
			seedSolver.getCallAutomaton().registerListener(new StackListener<Statement, INode<Val>, W>(seedSolver.getCallAutomaton(),
					new SingleNode<Val>(strongUpdateNode.fact()), strongUpdateNode.stmt()) {
				@Override
				public void anyContext(Statement end) {
				}

				@Override
				public void stackElement(Statement callSite) {
					boomerang.checkTimeout();
					addAffectedPotentialStrongUpdate(strongUpdateNode, callSite);
					for (ForwardQuery e : queryAllocationSites) {
						AbstractBoomerangSolver<W> solver = boomerang.getSolvers().get(e);
						solver.getCallAutomaton()
								.registerConnectPushListener(new IndirectFlowsAtCallSite(solver, callSite));
					}

				}
			});
		}

		private void injectAliasesAtStrongUpdates(Set<ForwardQuery> queryAllocationSites) {
			for (ForwardQuery e : queryAllocationSites) {
				AbstractBoomerangSolver<W> solver = boomerang.getSolvers().get(e);
				solver.getCallAutomaton().registerListener(new WPAUpdateListener<Statement, INode<Val>, W>() {
					@Override
					public void onWeightAdded(Transition<Statement, INode<Val>> t, W w,
							WeightedPAutomaton<Statement, INode<Val>, W> aut) {

						if (t.getLabel().equals(strongUpdateNode.stmt()) /* && !t.getStart().fact().equals(curr.fact()) */) {
							idealWeightFunctions.addNonKillFlow(strongUpdateNode);
							idealWeightFunctions.addIndirectFlow(strongUpdateNode,
									new Node<Statement,Val>(strongUpdateNode.stmt(), t.getStart().fact()));
						}
					}
				});
			}
		}

		private void setWeakUpdateIfNecessary() {
			for (final Entry<Query, AbstractBoomerangSolver<W>> e : boomerang.getSolvers().entrySet()) {
				if (e.getKey() instanceof ForwardQuery) {
					e.getValue().synchedEmptyStackReachable(strongUpdateNode, new EmptyStackWitnessListener<Statement, Val>() {
						@Override
						public void witnessFound(Node<Statement, Val> targetFact) {
							if (!e.getKey().asNode().equals(seed.asNode())) {
								setWeakUpdate(strongUpdateNode);
							}
						}
					});
				}
			}
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
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
			throw new IDEALSeedTimeout(this, this.phase1Solver, resultPhase1);
		}
		ForwardBoomerangResults<W> resultPhase2 = runPhase(this.phase2Solver, Phases.ValueFlow);
		if (resultPhase2.isTimedout()) {
			if (analysisStopwatch.isRunning()) {
				analysisStopwatch.stop();
			}
			throw new IDEALSeedTimeout(this, this.phase2Solver, resultPhase2);
		}
		return resultPhase2;
	}

	private WeightedBoomerang<W> createSolver(Phases phase) {
		return new WeightedBoomerang<W>(analysisDefinition.boomerangOptions()) {
			@Override
			public ObservableICFG<Unit, SootMethod> icfg() {
				if (analysisDefinition.icfg == null){
//					analysisDefinition.icfg = new ObservableDynamicICFG<W>(this);
					//For Static ICFG use this line
					analysisDefinition.icfg = new ObservableStaticICFG(new JimpleBasedInterproceduralCFG(false));
				}
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

			@Override
			public SeedFactory<W> getSeedFactory() {
				return seedFactory;
			}
			@Override
			public boolean preventCallRuleAdd(ForwardQuery sourceQuery, Rule<Statement, INode<Val>, W> rule) {
				 if (phase.equals(Phases.ValueFlow) && sourceQuery.equals(seed)) {
					 if (preventStrongUpdateFlows(rule)) {
						 return true;
					 }
				 }
				 return false;
			}
			
		};
	}

	protected boolean preventStrongUpdateFlows(Rule<Statement, INode<Val>, W> rule) {
		if(rule.getS1().equals(rule.getS2())) {
			if (idealWeightFunctions.isStrongUpdateStatement(rule.getL2())) {
				if (idealWeightFunctions.isKillFlow(new Node<Statement, Val>(rule.getL2(), rule.getS2().fact()))) {
					return true;
				}
			}
		}
		if(rule instanceof PushRule) {
			PushRule<Statement, INode<Val>, W> pushRule = (PushRule<Statement, INode<Val>, W>) rule;
			Statement callSite = pushRule.getCallSite();
			if (idealWeightFunctions.isStrongUpdateStatement(callSite)) {
				if (idealWeightFunctions.isKillFlow(new Node<Statement, Val>(callSite, rule.getS1().fact()))) {
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

		if (phase.equals(Phases.ValueFlow)) {
			registerIndirectFlowListener(boomerang.getSolvers().getOrCreate(seed));
		}
		callAutomaton.registerConnectPushListener(new ConnectPushListener<Statement, INode<Val>, W>() {

			@Override
			public void connect(Statement predOfCall, Statement callSite, INode<Val> returnedFact, W w) {
				if (!callSite.getMethod().equals(returnedFact.fact().m()))
					return;
				if(!boomerang.getSolvers().getOrCreate(seed).valueUsedInStatement((Stmt) callSite.getUnit().get(), returnedFact.fact()))
					return;
				if (!w.equals(one)) {
					idealWeightFunctions.addOtherThanOneWeight(new Node<Statement, Val>(callSite, returnedFact.fact()));
				}
			}
		});

		idealWeightFunctions.registerListener(new NonOneFlowListener() {
			@Override
			public void nonOneFlow(final Node<Statement, Val> curr) {
				if (phase.equals(Phases.ValueFlow)) {
					return;
				}
				AbstractBoomerangSolver<W> seedSolver = boomerang.getSolvers().getOrCreate(seed);
				seedSolver.getFieldAutomaton().registerListener(
						new TriggerBackwardQuery(seedSolver, boomerang, curr));
			}
		});
		ForwardBoomerangResults<W> res = boomerang.solve((ForwardQuery) seed);
		if (phase.equals(Phases.ValueFlow)) {
			boomerang.debugOutput();
		}
		analysisStopwatch.stop();
		return res;
	}

	protected void addAffectedPotentialStrongUpdate(Node<Statement, Val> strongUpdateNode, Statement stmt) {
		if (affectedStrongUpdateStmt.put(strongUpdateNode, stmt)) {
			idealWeightFunctions.potentialStrongUpdate(stmt);
			if (weakUpdates.contains(strongUpdateNode)) {
				idealWeightFunctions.weakUpdate(stmt);
			}
		}
	}

	private void setWeakUpdate(Node<Statement, Val> curr) {
		if (weakUpdates.add(curr)) {
			for (Statement s : Lists.newArrayList(affectedStrongUpdateStmt.get(curr))) {
				idealWeightFunctions.weakUpdate(s);
			}
		}
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
				Collection<Node<Statement,Val>> indirectFlows = idealWeightFunctions.getAliasesFor(source);
				for (Node<Statement,Val>  indirectFlow : indirectFlows) {
					solver.addCallRule(new NormalRule<Statement,INode<Val>,W>(new SingleNode<Val>(source.fact()),source.stmt(),new SingleNode<Val>(indirectFlow.fact()),indirectFlow.stmt(),one));
					solver.addFieldRule(new NormalRule<Field,INode<Node<Statement,Val>>,W>(solver.asFieldFact(source),solver.fieldWildCard(),solver.asFieldFact(indirectFlow),solver.fieldWildCard(),one));
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

	public Query getSeed() {
		return seed;
	}
}
